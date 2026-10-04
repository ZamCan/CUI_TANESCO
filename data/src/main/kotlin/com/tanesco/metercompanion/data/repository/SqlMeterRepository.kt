package com.tanesco.metercompanion.data.repository

import com.tanesco.metercompanion.core.model.DomainError
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.MeterCategory
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.PhaseConfiguration
import com.tanesco.metercompanion.core.repository.MeterRepository
import com.tanesco.metercompanion.data.database.DatabaseSchema
import com.tanesco.metercompanion.data.database.SqlCursor
import com.tanesco.metercompanion.data.database.SqlDriver
import com.tanesco.metercompanion.data.database.SqlValue
import com.tanesco.metercompanion.data.database.selectFirstOrNull
import com.tanesco.metercompanion.data.database.selectRows
import com.tanesco.metercompanion.data.database.ValueCodecs.meterCategoryFromStorage
import com.tanesco.metercompanion.data.database.ValueCodecs.phaseFromStorage
import com.tanesco.metercompanion.data.database.ValueCodecs.toStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SQLite-backed [MeterRepository].
 *
 * The selected column order is derived from
 * [DatabaseSchema.Meters.ALL_COLUMNS] so that the positional reads in
 * [toMeterProfile] can never drift from the SELECT list.
 */
class SqlMeterRepository(
    private val driver: SqlDriver,
    private val ioDispatcher: CoroutineDispatcher,
) : MeterRepository {

    private val changes = ChangeSignal()

    private val selectColumns = DatabaseSchema.Meters.ALL_COLUMNS.joinToString(", ")

    override fun observeAll(): Flow<List<MeterProfile>> =
        changes.changes.map { ioDispatcher.read { queryAll() } }

    override fun observeById(id: String): Flow<MeterProfile?> =
        changes.changes.map { ioDispatcher.read { queryById(id) } }

    override suspend fun getAll(): List<MeterProfile> = ioDispatcher.read { queryAll() }

    override suspend fun getById(id: String): MeterProfile? = ioDispatcher.read { queryById(id) }

    override suspend fun upsert(profile: MeterProfile): DomainResult<Unit> = ioDispatcher.write {
        val result = runCatchingStorage {
            driver.execute(upsertStatement(), profile.toArguments())
            Unit
        }
        if (result is DomainResult.Success) changes.notifyChanged()
        result
    }

    override suspend fun archive(id: String): DomainResult<Unit> = ioDispatcher.write {
        val result = runCatchingStorage {
            val affected = driver.update(
                sql = """
                    UPDATE ${DatabaseSchema.Meters.TABLE}
                    SET ${DatabaseSchema.Meters.IS_ARCHIVED} = 1,
                        ${DatabaseSchema.Meters.IS_DEFAULT} = 0
                    WHERE ${DatabaseSchema.Meters.ID} = ?
                """.trimIndent(),
                arguments = listOf(SqlValue.Text(id)),
            )
            if (affected == 0) {
                return@runCatchingStorage DomainResult.Failure(
                    DomainError.NotFound(
                        code = "meter.not_found",
                        entity = "meter",
                        id = id,
                    ),
                )
            }
            Unit
        }
        if (result is DomainResult.Success) changes.notifyChanged()
        result
    }

    override suspend fun isAccountNumberInUse(
        normalizedAccountNumber: String,
        excludeProfileId: String?,
    ): Boolean = ioDispatcher.read {
        // Account numbers are compared in normalised (digits only) form, so this
        // query normalises in SQL rather than relying on stored formatting.
        driver.selectFirstOrNull(
            sql = """
                SELECT ${DatabaseSchema.Meters.ID} FROM ${DatabaseSchema.Meters.TABLE}
                WHERE REPLACE(REPLACE(${DatabaseSchema.Meters.ACCOUNT_NUMBER}, ' ', ''), '-', '') = ?
                  AND (? IS NULL OR ${DatabaseSchema.Meters.ID} <> ?)
                LIMIT 1
            """.trimIndent(),
            arguments = listOf(
                SqlValue.Text(normalizedAccountNumber),
                SqlValue.of(excludeProfileId),
                SqlValue.of(excludeProfileId),
            ),
        ) != null
    }

    private fun queryAll(): List<MeterProfile> = driver.selectRows(
        sql = """
            SELECT $selectColumns FROM ${DatabaseSchema.Meters.TABLE}
            ORDER BY ${DatabaseSchema.Meters.UPDATED_AT} DESC
        """.trimIndent(),
        transform = ::toMeterProfile,
    )

    private fun queryById(id: String): MeterProfile? = driver.selectFirstOrNull(
        sql = "SELECT $selectColumns FROM ${DatabaseSchema.Meters.TABLE} WHERE ${DatabaseSchema.Meters.ID} = ?",
        arguments = listOf(SqlValue.Text(id)),
        transform = ::toMeterProfile,
    )

    private fun upsertStatement(): String {
        val columns = DatabaseSchema.Meters.ALL_COLUMNS
        val placeholders = columns.joinToString(", ") { "?" }
        val updates = columns
            .filterNot { it == DatabaseSchema.Meters.ID }
            .joinToString(", ") { "$it = excluded.$it" }
        return """
            INSERT INTO ${DatabaseSchema.Meters.TABLE} ($columns)
            VALUES ($placeholders)
            ON CONFLICT(${DatabaseSchema.Meters.ID}) DO UPDATE SET $updates
        """.trimIndent()
    }

    private fun MeterProfile.toArguments(): List<SqlValue> = listOf(
        SqlValue.Text(id),
        SqlValue.Text(accountNumber),
        SqlValue.Text(displayName),
        SqlValue.Text(category.toStorage()),
        SqlValue.Text(phase.toStorage()),
        SqlValue.of(region),
        SqlValue.of(notes),
        SqlValue.LongValue(createdAtMillis),
        SqlValue.LongValue(updatedAtMillis),
        SqlValue.bool(isArchived),
    )
}

/** Maps a row selected in [DatabaseSchema.Meters.ALL_COLUMNS] order. */
internal fun toMeterProfile(cursor: SqlCursor): MeterProfile = MeterProfile(
    id = cursor.getString(0).orEmpty(),
    accountNumber = cursor.getString(1).orEmpty(),
    displayName = cursor.getString(2).orEmpty(),
    category = meterCategoryFromStorage(cursor.getString(3)),
    phase = phaseFromStorage(cursor.getString(4)),
    region = cursor.getString(5),
    notes = cursor.getString(6),
    createdAtMillis = cursor.getLong(7) ?: 0L,
    updatedAtMillis = cursor.getLong(8) ?: 0L,
    isArchived = (cursor.getInt(9) ?: 0) == 1,
)

/**
 * Runs [block], converting a platform exception into a [DomainError.Storage].
 *
 * Cancellation is rethrown: it is control flow, and swallowing it would break
 * structured concurrency.
 */
internal inline fun <T> runCatchingStorage(block: () -> T): DomainResult<T> = try {
    DomainResult.Success(block())
} catch (cancellation: kotlinx.coroutines.cancellation.CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    DomainResult.Failure(
        DomainError.Storage(
            code = "storage.write_failed",
            detail = throwable::class.java.simpleName,
            cause = throwable,
        ),
    )
}


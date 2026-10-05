package com.tanesco.metercompanion.data.repository

import com.tanesco.metercompanion.core.model.DomainError
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.MeterSession
import com.tanesco.metercompanion.core.model.SessionOutcome
import com.tanesco.metercompanion.core.repository.MeterSessionRepository
import com.tanesco.metercompanion.data.database.DatabaseSchema
import com.tanesco.metercompanion.data.database.SqlCursor
import com.tanesco.metercompanion.data.database.SqlDriver
import com.tanesco.metercompanion.data.database.SqlValue
import com.tanesco.metercompanion.data.database.ValueCodecs.sessionOutcomeFromStorage
import com.tanesco.metercompanion.data.database.ValueCodecs.toStorage
import com.tanesco.metercompanion.data.database.selectFirstOrNull
import com.tanesco.metercompanion.data.database.selectRows
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SQLite-backed [MeterSessionRepository].
 *
 * Rows are inserted when an attempt starts and updated when it ends, so an
 * attempt interrupted by process death stays visible as an unfinished session
 * rather than disappearing. [getActive] is what lets Diagnostics report that
 * honestly instead of assuming nothing happened.
 */
class SqlMeterSessionRepository(
    private val driver: SqlDriver,
    private val ioDispatcher: CoroutineDispatcher,
) : MeterSessionRepository {

    private val changes = ChangeSignal()

    private val selectColumns = DatabaseSchema.MeterSessions.ALL_COLUMNS.joinToString(", ")

    override suspend fun start(session: MeterSession): DomainResult<Unit> = ioDispatcher.write {
        val result = runCatchingStorage {
            driver.execute(
                sql = """
                    INSERT INTO ${DatabaseSchema.MeterSessions.TABLE}
                        (${DatabaseSchema.MeterSessions.ALL_COLUMNS.joinToString(", ")})
                    VALUES (${DatabaseSchema.MeterSessions.ALL_COLUMNS.joinToString(", ") { "?" }})
                    ON CONFLICT(${DatabaseSchema.MeterSessions.ID}) DO NOTHING
                """.trimIndent(),
                arguments = listOf(
                    SqlValue.Text(session.id),
                    SqlValue.of(session.meterId),
                    SqlValue.Text(session.adapterId),
                    SqlValue.LongValue(session.startedAtMillis),
                    SqlValue.of(session.endedAtMillis),
                    SqlValue.Text(session.outcome.toStorage()),
                    SqlValue.of(session.failureCode),
                ),
            )
            Unit
        }
        if (result is DomainResult.Success) changes.notifyChanged()
        result
    }

    override suspend fun finish(
        id: String,
        endedAtMillis: Long,
        outcome: SessionOutcome,
        failureCode: String?,
    ): DomainResult<Unit> = ioDispatcher.write {
        val affected = runCatchingStorage {
            driver.update(
                sql = """
                    UPDATE ${DatabaseSchema.MeterSessions.TABLE}
                    SET ${DatabaseSchema.MeterSessions.ENDED_AT} = ?,
                        ${DatabaseSchema.MeterSessions.OUTCOME} = ?,
                        ${DatabaseSchema.MeterSessions.FAILURE_CODE} = ?
                    WHERE ${DatabaseSchema.MeterSessions.ID} = ?
                """.trimIndent(),
                arguments = listOf(
                    SqlValue.LongValue(endedAtMillis),
                    SqlValue.Text(outcome.toStorage()),
                    SqlValue.of(failureCode),
                    SqlValue.Text(id),
                ),
            )
        }

        val result = affected.fold(
            onSuccess = { updated ->
                if (updated == 0) {
                    DomainResult.Failure(
                        DomainError.NotFound(
                            code = "session.not_found",
                            entity = "meter session",
                            id = id,
                        ),
                    )
                } else {
                    DomainResult.Success(Unit)
                }
            },
            onFailure = { error -> DomainResult.Failure(error) },
        )

        if (result is DomainResult.Success) changes.notifyChanged()
        result
    }

    override fun observeRecent(limit: Int): Flow<List<MeterSession>> =
        changes.changes.map { ioDispatcher.read { queryRecent(limit) } }

    override suspend fun getRecent(limit: Int): List<MeterSession> = ioDispatcher.read { queryRecent(limit) }

    override suspend fun getActive(meterId: String?): MeterSession? = ioDispatcher.read {
        driver.selectFirstOrNull(
            sql = """
                SELECT $selectColumns FROM ${DatabaseSchema.MeterSessions.TABLE}
                WHERE ${DatabaseSchema.MeterSessions.ENDED_AT} IS NULL
                  AND (? IS NULL OR ${DatabaseSchema.MeterSessions.METER_ID} = ?)
                ORDER BY ${DatabaseSchema.MeterSessions.STARTED_AT} DESC
                LIMIT 1
            """.trimIndent(),
            arguments = listOf(
                SqlValue.of(meterId),
                SqlValue.of(meterId),
            ),
            transform = ::toMeterSession,
        )
    }

    private fun queryRecent(limit: Int): List<MeterSession> {
        // Interpolated rather than bound; see SqlEventRepository.runQuery.
        val queryLimit = limit
        return driver.selectRows(
            sql = """
                SELECT $selectColumns FROM ${DatabaseSchema.MeterSessions.TABLE}
                ORDER BY ${DatabaseSchema.MeterSessions.STARTED_AT} DESC
                LIMIT $queryLimit
            """.trimIndent(),
            arguments = emptyList(),
            transform = ::toMeterSession,
        )
    }
}

/** Maps a row selected in [DatabaseSchema.MeterSessions.ALL_COLUMNS] order. */
internal fun toMeterSession(cursor: SqlCursor): MeterSession = MeterSession(
    id = cursor.getString(0).orEmpty(),
    meterId = cursor.getString(1),
    adapterId = cursor.getString(2).orEmpty(),
    startedAtMillis = cursor.getLong(3) ?: 0L,
    endedAtMillis = cursor.getLong(4),
    outcome = sessionOutcomeFromStorage(cursor.getString(5)),
    failureCode = cursor.getString(6),
)

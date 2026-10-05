package com.tanesco.metercompanion.data.repository

import com.tanesco.metercompanion.core.model.DomainError
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.TokenHistory
import com.tanesco.metercompanion.core.model.TokenQuery
import com.tanesco.metercompanion.core.model.TokenRecord
import com.tanesco.metercompanion.core.model.TokenTransferState
import com.tanesco.metercompanion.core.repository.TokenRepository
import com.tanesco.metercompanion.data.database.DatabaseSchema
import com.tanesco.metercompanion.data.database.SqlCursor
import com.tanesco.metercompanion.data.database.SqlDriver
import com.tanesco.metercompanion.data.database.SqlValue
import com.tanesco.metercompanion.data.database.ValueCodecs.decodeValidation
import com.tanesco.metercompanion.data.database.ValueCodecs.encodeValidation
import com.tanesco.metercompanion.data.database.ValueCodecs.toStorage
import com.tanesco.metercompanion.data.database.ValueCodecs.tokenStatusFromStorage
import com.tanesco.metercompanion.data.database.ValueCodecs.transferStateFromStorage
import com.tanesco.metercompanion.data.database.selectFirstOrNull
import com.tanesco.metercompanion.data.database.selectRows
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SQLite-backed [TokenRepository].
 */
class SqlTokenRepository(
    private val driver: SqlDriver,
    private val ioDispatcher: CoroutineDispatcher,
) : TokenRepository {

    private val changes = ChangeSignal()

    private val selectColumns = DatabaseSchema.Tokens.ALL_COLUMNS.joinToString(", ")

    override fun observeHistory(query: TokenQuery): Flow<TokenHistory> =
        changes.changes.map { ioDispatcher.read { queryHistory(query) } }

    override suspend fun getHistory(query: TokenQuery): TokenHistory = ioDispatcher.read { queryHistory(query) }

    override suspend fun getById(id: String): TokenRecord? = ioDispatcher.read {
        driver.selectFirstOrNull(
            sql = "SELECT $selectColumns FROM ${DatabaseSchema.Tokens.TABLE} WHERE ${DatabaseSchema.Tokens.ID} = ?",
            arguments = listOf(SqlValue.Text(id)),
            transform = ::toTokenRecord,
        )
    }

    override suspend fun findPreviousWithSameValue(
        normalizedValue: String,
        beforeMillis: Long,
        excludeTokenId: String?,
    ): TokenRecord? = ioDispatcher.read {
        driver.selectFirstOrNull(
            sql = """
                SELECT $selectColumns FROM ${DatabaseSchema.Tokens.TABLE}
                WHERE ${DatabaseSchema.Tokens.NORMALIZED_VALUE} = ?
                  AND ${DatabaseSchema.Tokens.ENTERED_AT} <= ?
                  AND (? IS NULL OR ${DatabaseSchema.Tokens.ID} <> ?)
                ORDER BY ${DatabaseSchema.Tokens.ENTERED_AT} ASC
                LIMIT 1
            """.trimIndent(),
            arguments = listOf(
                SqlValue.Text(normalizedValue),
                SqlValue.LongValue(beforeMillis),
                SqlValue.of(excludeTokenId),
                SqlValue.of(excludeTokenId),
            ),
            transform = ::toTokenRecord,
        )
    }

    override suspend fun insert(record: TokenRecord): DomainResult<Unit> = ioDispatcher.write {
        val result = runCatchingStorage {
            driver.execute(
                sql = """
                    INSERT INTO ${DatabaseSchema.Tokens.TABLE}
                        (${DatabaseSchema.Tokens.ALL_COLUMNS.joinToString(", ")})
                    VALUES (${DatabaseSchema.Tokens.ALL_COLUMNS.joinToString(", ") { "?" }})
                    ON CONFLICT(${DatabaseSchema.Tokens.ID}) DO UPDATE SET
                        ${DatabaseSchema.Tokens.NORMALIZED_VALUE} = excluded.${DatabaseSchema.Tokens.NORMALIZED_VALUE},
                        ${DatabaseSchema.Tokens.DISPLAY_VALUE} = excluded.${DatabaseSchema.Tokens.DISPLAY_VALUE},
                        ${DatabaseSchema.Tokens.STATUS} = excluded.${DatabaseSchema.Tokens.STATUS},
                        ${DatabaseSchema.Tokens.METER_ID} = excluded.${DatabaseSchema.Tokens.METER_ID},
                        ${DatabaseSchema.Tokens.TRANSFER_STATE} = excluded.${DatabaseSchema.Tokens.TRANSFER_STATE},
                        ${DatabaseSchema.Tokens.NOTE} = excluded.${DatabaseSchema.Tokens.NOTE},
                        ${DatabaseSchema.Tokens.VALIDATION} = excluded.${DatabaseSchema.Tokens.VALIDATION}
                """.trimIndent(),
                arguments = record.toArguments(),
            )
            Unit
        }
        if (result is DomainResult.Success) changes.notifyChanged()
        result
    }

    override suspend fun setTransferState(id: String, state: TokenTransferState): DomainResult<Unit> =
        ioDispatcher.write {
            val affected = runCatchingStorage {
                driver.update(
                    sql = """
                        UPDATE ${DatabaseSchema.Tokens.TABLE}
                        SET ${DatabaseSchema.Tokens.TRANSFER_STATE} = ?
                        WHERE ${DatabaseSchema.Tokens.ID} = ?
                    """.trimIndent(),
                    arguments = listOf(SqlValue.Text(state.toStorage()), SqlValue.Text(id)),
                )
            }

            val result = affected.fold(
                onSuccess = { updated ->
                    if (updated == 0) {
                        DomainResult.Failure(
                            DomainError.NotFound(
                                code = "token.not_found",
                                entity = "token",
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

    private fun queryHistory(query: TokenQuery): TokenHistory {
        val filters = mutableListOf<String>()
        val arguments = mutableListOf<SqlValue>()

        query.meterId?.let {
            filters += "${DatabaseSchema.Tokens.METER_ID} = ?"
            arguments += SqlValue.Text(it)
        }
        query.status?.let {
            filters += "${DatabaseSchema.Tokens.STATUS} = ?"
            arguments += SqlValue.Text(it.toStorage())
        }
        query.sinceMillis?.let {
            filters += "${DatabaseSchema.Tokens.ENTERED_AT} >= ?"
            arguments += SqlValue.LongValue(it)
        }

        val whereClause = if (filters.isEmpty()) "" else "WHERE " + filters.joinToString(" AND ")

        // Interpolated rather than bound; see SqlEventRepository.runQuery.
        val queryLimit = query.limit

        // The total is counted without the LIMIT so the UI can honestly say
        // "showing N of M".
        val total = driver.selectFirstOrNull(
            sql = "SELECT COUNT(*) FROM ${DatabaseSchema.Tokens.TABLE} $whereClause",
            arguments = arguments,
            transform = { cursor -> cursor.getLong(0) ?: 0L },
        ) ?: 0L

        val rows = driver.selectRows(
            sql = """
                SELECT $selectColumns FROM ${DatabaseSchema.Tokens.TABLE}
                $whereClause
                ORDER BY ${DatabaseSchema.Tokens.ENTERED_AT} DESC
                LIMIT $queryLimit
            """.trimIndent(),
            arguments = arguments,
            transform = ::toTokenRecord,
        )

        return TokenHistory(records = rows, totalCount = total.toInt())
    }

    private fun TokenRecord.toArguments(): List<SqlValue> = listOf(
        SqlValue.Text(id),
        SqlValue.Text(normalizedValue),
        SqlValue.Text(displayValue),
        SqlValue.Text(status.toStorage()),
        SqlValue.of(meterId),
        SqlValue.LongValue(enteredAtMillis),
        SqlValue.Text(transferState.toStorage()),
        SqlValue.of(note),
        SqlValue.of(encodeValidation(validation)),
    )
}

/** Maps a row selected in [DatabaseSchema.Tokens.ALL_COLUMNS] order. */
internal fun toTokenRecord(cursor: SqlCursor): TokenRecord = TokenRecord(
    id = cursor.getString(0).orEmpty(),
    normalizedValue = cursor.getString(1).orEmpty(),
    displayValue = cursor.getString(2).orEmpty(),
    status = tokenStatusFromStorage(cursor.getString(3)),
    meterId = cursor.getString(4),
    enteredAtMillis = cursor.getLong(5) ?: 0L,
    transferState = transferStateFromStorage(cursor.getString(6)),
    note = cursor.getString(7),
    validation = decodeValidation(cursor.getString(8)),
)

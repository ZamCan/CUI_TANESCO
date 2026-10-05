package com.tanesco.metercompanion.data.repository

import com.tanesco.metercompanion.core.model.AppEvent
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.EventQuery
import com.tanesco.metercompanion.core.repository.EventRepository
import com.tanesco.metercompanion.data.database.DatabaseSchema
import com.tanesco.metercompanion.data.database.SqlCursor
import com.tanesco.metercompanion.data.database.SqlDriver
import com.tanesco.metercompanion.data.database.SqlValue
import com.tanesco.metercompanion.data.database.ValueCodecs.eventCategoryFromStorage
import com.tanesco.metercompanion.data.database.ValueCodecs.eventSeverityFromStorage
import com.tanesco.metercompanion.data.database.ValueCodecs.toStorage
import com.tanesco.metercompanion.data.database.selectFirstOrNull
import com.tanesco.metercompanion.data.database.selectRows
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SQLite-backed [EventRepository].
 *
 * The log is append-only. There is deliberately no update or per-event delete;
 * [prune] exists so a user can reclaim space, and it is the only mutation that
 * removes rows.
 */
class SqlEventRepository(
    private val driver: SqlDriver,
    private val ioDispatcher: CoroutineDispatcher,
) : EventRepository {

    private val changes = ChangeSignal()

    private val selectColumns = DatabaseSchema.Events.ALL_COLUMNS.joinToString(", ")

    override suspend fun append(event: AppEvent) = ioDispatcher.write {
        val result = runCatchingStorage {
            driver.execute(
                sql = """
                    INSERT INTO ${DatabaseSchema.Events.TABLE}
                        (${DatabaseSchema.Events.ALL_COLUMNS.joinToString(", ")})
                    VALUES (${DatabaseSchema.Events.ALL_COLUMNS.joinToString(", ") { "?" }})
                    ON CONFLICT(${DatabaseSchema.Events.ID}) DO NOTHING
                """.trimIndent(),
                arguments = listOf(
                    SqlValue.Text(event.id),
                    SqlValue.Text(event.category.toStorage()),
                    SqlValue.Text(event.name),
                    SqlValue.Text(event.severity.toStorage()),
                    SqlValue.LongValue(event.occurredAtMillis),
                    SqlValue.Text(event.summary),
                    SqlValue.of(event.detail),
                    SqlValue.of(event.meterId),
                    SqlValue.bool(event.userInitiated),
                ),
            )
            Unit
        }
        if (result is DomainResult.Success) changes.notifyChanged()
        result
    }

    override suspend fun query(query: EventQuery): List<AppEvent> = ioDispatcher.read { runQuery(query) }

    override fun observe(query: EventQuery): Flow<List<AppEvent>> =
        changes.changes.map { ioDispatcher.read { runQuery(query) } }

    override suspend fun countByName(name: String): Int = ioDispatcher.read {
        driver.selectFirstOrNull(
            sql = """
                SELECT COUNT(*) FROM ${DatabaseSchema.Events.TABLE}
                WHERE ${DatabaseSchema.Events.NAME} = ?
            """.trimIndent(),
            arguments = listOf(SqlValue.Text(name)),
            transform = { cursor -> cursor.getLong(0) ?: 0L },
        )?.toInt() ?: 0
    }

    override suspend fun prune(olderThanMillis: Long): Int = ioDispatcher.write {
        val removed = runCatchingStorage {
            driver.delete(
                sql = "DELETE FROM ${DatabaseSchema.Events.TABLE} WHERE ${DatabaseSchema.Events.OCCURRED_AT} < ?",
                arguments = listOf(SqlValue.LongValue(olderThanMillis)),
            )
        }
        if (removed is DomainResult.Success) changes.notifyChanged()
        (removed as? DomainResult.Success)?.value ?: 0
    }

    private fun runQuery(query: EventQuery): List<AppEvent> {
        // Interpolated rather than bound: Android's query API accepts only
        // string arguments, and `query.limit` is validated to be a positive Int
        // by EventQuery's own initializer, so it cannot carry SQL.
        val queryLimit = query.limit
        val filters = mutableListOf<String>()
        val arguments = mutableListOf<SqlValue>()

        query.category?.let {
            filters += "${DatabaseSchema.Events.CATEGORY} = ?"
            arguments += SqlValue.Text(it.toStorage())
        }
        query.meterId?.let {
            filters += "${DatabaseSchema.Events.METER_ID} = ?"
            arguments += SqlValue.Text(it)
        }
        query.sinceMillis?.let {
            filters += "${DatabaseSchema.Events.OCCURRED_AT} >= ?"
            arguments += SqlValue.LongValue(it)
        }

        val whereClause = if (filters.isEmpty()) "" else "WHERE " + filters.joinToString(" AND ")

        return driver.selectRows(
            sql = """
                SELECT $selectColumns FROM ${DatabaseSchema.Events.TABLE}
                $whereClause
                ORDER BY ${DatabaseSchema.Events.OCCURRED_AT} DESC
                LIMIT $queryLimit
            """.trimIndent(),
            arguments = arguments,
            transform = ::toAppEvent,
        )
    }
}

/** Maps a row selected in [DatabaseSchema.Events.ALL_COLUMNS] order. */
internal fun toAppEvent(cursor: SqlCursor): AppEvent = AppEvent(
    id = cursor.getString(0).orEmpty(),
    category = eventCategoryFromStorage(cursor.getString(1)),
    name = cursor.getString(2).orEmpty(),
    severity = eventSeverityFromStorage(cursor.getString(3)),
    occurredAtMillis = cursor.getLong(4) ?: 0L,
    summary = cursor.getString(5).orEmpty(),
    detail = cursor.getString(6),
    meterId = cursor.getString(7),
    userInitiated = (cursor.getInt(8) ?: 0) == 1,
)

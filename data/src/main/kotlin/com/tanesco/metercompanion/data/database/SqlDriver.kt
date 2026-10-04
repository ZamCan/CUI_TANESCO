package com.tanesco.metercompanion.data.database

/**
 * A value that can be bound to a SQL statement.
 *
 * A closed hierarchy rather than `Any?` so that an unsupported type is a
 * compile error instead of a runtime failure deep inside the driver.
 */
sealed interface SqlValue {

    data class Text(val value: String) : SqlValue
    data class IntValue(val value: Int) : SqlValue
    data class LongValue(val value: Long) : SqlValue
    data class DoubleValue(val value: Double) : SqlValue
    data class BlobValue(val value: ByteArray) : SqlValue
    data object Null : SqlValue

    companion object {
        fun of(value: String?): SqlValue = value?.let { Text(it) } ?: Null
        fun of(value: Long?): SqlValue = value?.let { LongValue(it) } ?: Null
        fun of(value: Int?): SqlValue = value?.let { IntValue(it) } ?: Null
        fun of(value: Double?): SqlValue = value?.let { DoubleValue(it) } ?: Null
        fun bool(value: Boolean): SqlValue = LongValue(if (value) 1L else 0L)
    }
}

/**
 * Forward-only reader over a result set.
 *
 * Mirrors only what the repositories actually need. Keeping this narrow is what
 * allows the Android SQLite implementation and an in-memory test double to be
 * genuinely interchangeable.
 */
interface SqlCursor {

    val columnCount: Int

    /** Advances to the next row. Returns `false` when the set is exhausted. */
    fun next(): Boolean

    fun isNull(index: Int): Boolean

    fun getString(index: Int): String?

    fun getLong(index: Int): Long?

    fun getInt(index: Int): Int?

    fun getDouble(index: Int): Double?

    /** Releases the result set. Safe to call more than once. */
    fun close()
}

/**
 * The persistence port used by the repositories.
 *
 * This is the only database abstraction in the project. It exists so that the
 * repository logic is ordinary Kotlin that can be unit tested on a JVM, while
 * the concrete implementation uses Android's built-in SQLite - a platform
 * capability, not a third-party dependency. See docs/decisions/0003-persistence.md.
 *
 * Implementations are **not** required to be thread safe; callers serialise
 * access through a single-threaded dispatcher (see :platform).
 */
interface SqlDriver {

    /** Runs a statement that returns no rows. */
    fun execute(sql: String, arguments: List<SqlValue> = emptyList())

    /** Runs an INSERT and returns the new row id. */
    fun insert(sql: String, arguments: List<SqlValue> = emptyList()): Long

    /** Runs an UPDATE and returns the number of affected rows. */
    fun update(sql: String, arguments: List<SqlValue> = emptyList()): Int

    /** Runs a DELETE and returns the number of affected rows. */
    fun delete(sql: String, arguments: List<SqlValue> = emptyList()): Int

    /** Opens a result set. The caller must consume or close it. */
    fun query(sql: String, arguments: List<SqlValue> = emptyList()): SqlCursor

    /** Runs [block] inside a transaction, committing on success. */
    fun <T> transaction(block: SqlDriver.() -> T): T

    /** Releases any underlying resources. */
    fun close()
}

/**
 * Reads every row produced by [this] cursor with [transform].
 *
 * The cursor is always closed, including when [transform] throws, so a malformed
 * row cannot leak a native cursor.
 */
inline fun <T> SqlCursor.mapRows(transform: (SqlCursor) -> T): List<T> {
    val results = mutableListOf<T>()
    try {
        while (next()) {
            results.add(transform(this))
        }
    } finally {
        close()
    }
    return results
}

/** Maps the first row, or returns `null` when the result set is empty. */
inline fun <T> SqlCursor.firstRowOrNull(transform: (SqlCursor) -> T): T? =
    mapRows(transform).firstOrNull()

/** Convenience wrapper around [SqlDriver.query] that materialises the result. */
inline fun <T> SqlDriver.selectRows(
    sql: String,
    arguments: List<SqlValue> = emptyList(),
    transform: (SqlCursor) -> T,
): List<T> = query(sql, arguments).mapRows(transform)

/** Convenience wrapper returning the first row or `null`. */
inline fun <T> SqlDriver.selectFirstOrNull(
    sql: String,
    arguments: List<SqlValue> = emptyList(),
    transform: (SqlCursor) -> T,
): T? = query(sql, arguments).firstRowOrNull(transform)

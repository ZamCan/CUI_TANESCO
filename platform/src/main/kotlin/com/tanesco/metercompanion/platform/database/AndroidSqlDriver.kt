package com.tanesco.metercompanion.platform.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.tanesco.metercompanion.data.database.DatabaseSchema
import com.tanesco.metercompanion.data.database.SqlCursor
import com.tanesco.metercompanion.data.database.SqlDriver
import com.tanesco.metercompanion.data.database.SqlValue

/**
 * [SqlDriver] implemented on Android's SQLite.
 *
 * Thread safety: the underlying [SQLiteDatabase] is thread safe, but callers
 * serialise access through a single-threaded dispatcher anyway so that
 * transactions cannot interleave unpredictably. See
 * [com.tanesco.metercompanion.platform.AndroidDispatchers].
 */
class AndroidSqlDriver(
    context: Context,
    databaseName: String = DatabaseSchema.DATABASE_NAME,
) : SqlDriver {

    private val helper = TanescoDatabaseHelper(context.applicationContext, databaseName)

    /**
     * Resolved lazily so that merely constructing the driver does not create
     * the database file. Opening is deferred to the first real query.
     */
    private val database: SQLiteDatabase
        get() = helper.writableDatabase

    override fun execute(sql: String, arguments: List<SqlValue>) {
        database.execSQL(sql, arguments.toBindArgs())
    }

    /**
     * Runs an INSERT statement and returns the new row id.
     *
     * Android's `insertWithOnConflict` takes a table name rather than a full
     * statement, so the statement is executed directly and the row id is read
     * back from SQLite. Statements with an `ON CONFLICT` clause - which most
     * inserts in this application use - cannot be expressed through
     * `insertWithOnConflict` at all.
     */
    override fun insert(sql: String, arguments: List<SqlValue>): Long {
        val db = database
        db.execSQL(sql, arguments.toBindArgs())
        db.rawQuery(LAST_INSERT_ROW_ID, null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else -1L
        }
    }

    override fun update(sql: String, arguments: List<SqlValue>): Int =
        database.update(sql, null, arguments.toBindArgs())

    override fun delete(sql: String, arguments: List<SqlValue>): Int =
        database.delete(sql, arguments.toBindArgs())

    override fun query(sql: String, arguments: List<SqlValue>): SqlCursor =
        AndroidSqlCursor(database.rawQuery(sql, arguments.toBindArgs()))

    override fun <T> transaction(block: SqlDriver.() -> T): T {
        val db = database
        db.beginTransaction()
        return try {
            val result = block()
            db.setTransactionSuccessful()
            result
        } finally {
            db.endTransaction()
        }
    }

    override fun close() {
        helper.close()
    }
}

private const val LAST_INSERT_ROW_ID = "SELECT last_insert_rowid()"

/**
 * Adapts an Android [Cursor] to the portable [SqlCursor] port.
 *
 * `close()` is idempotent because [com.tanesco.metercompanion.data.database.mapRows]
 * guarantees it is called on every path, including failure.
 */
internal class AndroidSqlCursor(private val cursor: Cursor) : SqlCursor {

    private var closed = false

    override val columnCount: Int get() = cursor.columnCount

    override fun next(): Boolean = cursor.moveToNext()

    override fun isNull(index: Int): Boolean = cursor.isNull(index)

    override fun getString(index: Int): String? =
        if (cursor.isNull(index)) null else cursor.getString(index)

    override fun getLong(index: Int): Long? =
        if (cursor.isNull(index)) null else cursor.getLong(index)

    override fun getInt(index: Int): Int? =
        if (cursor.isNull(index)) null else cursor.getInt(index)

    override fun getDouble(index: Int): Double? =
        if (cursor.isNull(index)) null else cursor.getDouble(index)

    override fun close() {
        if (!closed) {
            closed = true
            cursor.close()
        }
    }
}

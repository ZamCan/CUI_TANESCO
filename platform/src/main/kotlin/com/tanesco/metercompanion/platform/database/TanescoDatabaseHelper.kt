package com.tanesco.metercompanion.platform.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.tanesco.metercompanion.data.database.DatabaseSchema
import com.tanesco.metercompanion.data.database.SqlValue

/**
 * Creates and upgrades the local database using Android's built-in SQLite.
 *
 * No third-party database library is involved: SQLite is a platform capability,
 * so using it directly removes an annotation processor from the build and keeps
 * the APK small. The trade-offs are recorded in
 * docs/decisions/0003-persistence.md.
 */
class TanescoDatabaseHelper(
    context: Context,
    databaseName: String = DatabaseSchema.DATABASE_NAME,
) : SQLiteOpenHelper(context, databaseName, null, DatabaseSchema.VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Referential integrity is enforced by SQLite itself rather than by
        // application code, so a bug cannot leave orphaned rows behind.
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        DatabaseSchema.CREATE_STATEMENTS.forEach(db::execSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrations are explicit. Applying schema changes "best effort" to a
        // user's existing data is never acceptable, so an unknown starting
        // version fails loudly instead of guessing.
        var version = oldVersion
        while (version < newVersion) {
            val migration = DatabaseSchema.MIGRATIONS.firstOrNull { it.fromVersion == version }
                ?: throw IllegalStateException(
                    "No migration registered from database version $version to ${version + 1}. " +
                        "Add a DatabaseSchema.Migration entry before bumping DatabaseSchema.VERSION.",
                )
            migration.statements.forEach(db::execSQL)
            version++
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // A downgrade means the user moved to an older build. Data is preserved;
        // the newer schema's unknown columns are simply ignored by older code.
        super.onDowngrade(db, oldVersion, newVersion)
    }
}

/** Converts [SqlValue] into the `Array<Any?>` Android's binding API expects. */
internal fun List<SqlValue>.toBindArgs(): Array<Any?> = map { value ->
    when (value) {
        is SqlValue.Text -> value.value
        is SqlValue.IntValue -> value.value
        is SqlValue.LongValue -> value.value
        is SqlValue.DoubleValue -> value.value
        is SqlValue.BlobValue -> value.value
        SqlValue.Null -> null
    }
}.toTypedArray()

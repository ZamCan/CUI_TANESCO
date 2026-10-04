package com.tanesco.metercompanion.data.database

/**
 * The database contract: table and column names, the schema version, and the
 * statements that create the database from nothing.
 *
 * Names are declared once as constants because they are referenced from the
 * repositories, from the migrations and from tests. A rename that is not
 * reflected here would silently break a migration.
 */
object DatabaseSchema {

    /**
     * Current schema version.
     *
     * Bump this together with a new [Migration] entry. The version is stored in
     * SQLite's `user_version` pragma.
     */
    const val VERSION = 1

    const val DATABASE_NAME = "tanesco_meter_companion.db"

    // --- meters ------------------------------------------------------------
    object Meters {
        const val TABLE = "meters"
        const val ID = "id"
        const val ACCOUNT_NUMBER = "account_number"
        const val DISPLAY_NAME = "display_name"
        const val CATEGORY = "category"
        const val PHASE = "phase"
        const val REGION = "region"
        const val NOTES = "notes"
        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
        const val IS_ARCHIVED = "is_archived"

        // Note: there is deliberately no `is_default` column. The selected
        // default meter is a user *setting*, not a property of the meter, and
        // keeping it in AppSettings avoids a second source of truth that could
        // disagree with itself.
        val ALL_COLUMNS = listOf(
            ID, ACCOUNT_NUMBER, DISPLAY_NAME, CATEGORY, PHASE, REGION, NOTES,
            CREATED_AT, UPDATED_AT, IS_ARCHIVED,
        )
    }

    // --- meter_sessions ----------------------------------------------------
    object MeterSessions {
        const val TABLE = "meter_sessions"
        const val ID = "id"
        const val METER_ID = "meter_id"
        const val ADAPTER_ID = "adapter_id"
        const val STARTED_AT = "started_at"
        const val ENDED_AT = "ended_at"
        const val OUTCOME = "outcome"
        const val FAILURE_CODE = "failure_code"

        val ALL_COLUMNS = listOf(
            ID, METER_ID, ADAPTER_ID, STARTED_AT, ENDED_AT, OUTCOME, FAILURE_CODE,
        )
    }

    // --- tokens ------------------------------------------------------------
    object Tokens {
        const val TABLE = "tokens"
        const val ID = "id"
        const val NORMALIZED_VALUE = "normalized_value"
        const val DISPLAY_VALUE = "display_value"
        const val STATUS = "status"
        const val METER_ID = "meter_id"
        const val ENTERED_AT = "entered_at"
        const val TRANSFER_STATE = "transfer_state"
        const val NOTE = "note"
        /** Encoded [com.tanesco.metercompanion.core.model.TokenValidationReport]. */
        const val VALIDATION = "validation"

        val ALL_COLUMNS = listOf(
            ID, NORMALIZED_VALUE, DISPLAY_VALUE, STATUS, METER_ID, ENTERED_AT,
            TRANSFER_STATE, NOTE, VALIDATION,
        )
    }

    // --- events ------------------------------------------------------------
    object Events {
        const val TABLE = "events"
        const val ID = "id"
        const val CATEGORY = "category"
        const val NAME = "name"
        const val SEVERITY = "severity"
        const val OCCURRED_AT = "occurred_at"
        const val SUMMARY = "summary"
        const val DETAIL = "detail"
        const val METER_ID = "meter_id"
        const val USER_INITIATED = "user_initiated"

        val ALL_COLUMNS = listOf(
            ID, CATEGORY, NAME, SEVERITY, OCCURRED_AT, SUMMARY, DETAIL, METER_ID,
            USER_INITIATED,
        )
    }

    // --- settings ----------------------------------------------------------
    object Settings {
        const val TABLE = "settings"
        const val KEY = "key"
        const val VALUE = "value"
    }

    /**
     * Statements executed in order to create an empty database at [VERSION].
     *
     * Written as data rather than as code so the same list can be asserted on in
     * a unit test and reused by the Android driver without duplicating SQL.
     */
    val CREATE_STATEMENTS: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS ${Meters.TABLE} (
            ${Meters.ID} TEXT PRIMARY KEY NOT NULL,
            ${Meters.ACCOUNT_NUMBER} TEXT NOT NULL,
            ${Meters.DISPLAY_NAME} TEXT NOT NULL,
            ${Meters.CATEGORY} TEXT NOT NULL,
            ${Meters.PHASE} TEXT NOT NULL,
            ${Meters.REGION} TEXT,
            ${Meters.NOTES} TEXT,
            ${Meters.CREATED_AT} INTEGER NOT NULL,
            ${Meters.UPDATED_AT} INTEGER NOT NULL,
            ${Meters.IS_ARCHIVED} INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS ${MeterSessions.TABLE} (
            ${MeterSessions.ID} TEXT PRIMARY KEY NOT NULL,
            ${MeterSessions.METER_ID} TEXT,
            ${MeterSessions.ADAPTER_ID} TEXT NOT NULL,
            ${MeterSessions.STARTED_AT} INTEGER NOT NULL,
            ${MeterSessions.ENDED_AT} INTEGER,
            ${MeterSessions.OUTCOME} TEXT NOT NULL,
            ${MeterSessions.FAILURE_CODE} TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS ${Tokens.TABLE} (
            ${Tokens.ID} TEXT PRIMARY KEY NOT NULL,
            ${Tokens.NORMALIZED_VALUE} TEXT NOT NULL,
            ${Tokens.DISPLAY_VALUE} TEXT NOT NULL,
            ${Tokens.STATUS} TEXT NOT NULL,
            ${Tokens.METER_ID} TEXT,
            ${Tokens.ENTERED_AT} INTEGER NOT NULL,
            ${Tokens.TRANSFER_STATE} TEXT NOT NULL,
            ${Tokens.NOTE} TEXT,
            ${Tokens.VALIDATION} TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS ${Events.TABLE} (
            ${Events.ID} TEXT PRIMARY KEY NOT NULL,
            ${Events.CATEGORY} TEXT NOT NULL,
            ${Events.NAME} TEXT NOT NULL,
            ${Events.SEVERITY} TEXT NOT NULL,
            ${Events.OCCURRED_AT} INTEGER NOT NULL,
            ${Events.SUMMARY} TEXT NOT NULL,
            ${Events.DETAIL} TEXT,
            ${Events.METER_ID} TEXT,
            ${Events.USER_INITIATED} INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS ${Settings.TABLE} (
            ${Settings.KEY} TEXT PRIMARY KEY NOT NULL,
            ${Settings.VALUE} TEXT NOT NULL
        )
        """.trimIndent(),
        // Indexes chosen from the queries the repositories actually issue.
        "CREATE INDEX IF NOT EXISTS idx_tokens_entered_at ON ${Tokens.TABLE} (${Tokens.ENTERED_AT} DESC)",
        "CREATE INDEX IF NOT EXISTS idx_tokens_normalized ON ${Tokens.TABLE} (${Tokens.NORMALIZED_VALUE})",
        "CREATE INDEX IF NOT EXISTS idx_tokens_meter ON ${Tokens.TABLE} (${Tokens.METER_ID})",
        "CREATE INDEX IF NOT EXISTS idx_events_occurred_at ON ${Events.TABLE} (${Events.OCCURRED_AT} DESC)",
        "CREATE INDEX IF NOT EXISTS idx_events_name ON ${Events.TABLE} (${Events.NAME})",
        "CREATE INDEX IF NOT EXISTS idx_sessions_started_at ON ${MeterSessions.TABLE} (${MeterSessions.STARTED_AT} DESC)",
    )

    /**
     * An upgrade step from [from] to [from] + 1.
     *
     * Migrations are explicit rather than inferred so that a schema change can
     * never be applied "best effort" to a user's existing data.
     */
    data class Migration(
        val fromVersion: Int,
        val statements: List<String>,
    )

    /**
     * Migrations keyed by source version.
     *
     * Empty at version 1 because the database is created directly at [VERSION].
     */
    val MIGRATIONS: List<Migration> = emptyList()
}

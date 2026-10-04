package com.tanesco.metercompanion.data.repository

import com.tanesco.metercompanion.core.model.AppLanguage
import com.tanesco.metercompanion.core.model.AppSettings
import com.tanesco.metercompanion.core.model.AppearanceMode
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.UnitSystem
import com.tanesco.metercompanion.core.repository.SettingsRepository
import com.tanesco.metercompanion.data.database.DatabaseSchema
import com.tanesco.metercompanion.data.database.SqlValue
import com.tanesco.metercompanion.data.database.selectFirstOrNull
import com.tanesco.metercompanion.data.database.selectRows
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SQLite-backed [SettingsRepository].
 *
 * Settings are stored one key per row rather than as a single serialised blob.
 * The trade-off is deliberate: a key/value table needs no serialisation format
 * to stay stable across versions, each field can be read independently, and a
 * partially written settings row degrades to defaults for the missing field
 * instead of failing to deserialise at all.
 *
 * Unknown keys written by a newer build are ignored on read and preserved on
 * write, so downgrading does not destroy a user's configuration.
 */
class SqlSettingsRepository(
    private val driver: SqlDriver,
    private val ioDispatcher: CoroutineDispatcher,
) : SettingsRepository {

    private val changes = ChangeSignal()

    override fun observe(): Flow<AppSettings> =
        changes.changes.map { ioDispatcher.read { readSettings() } }

    override suspend fun get(): AppSettings = ioDispatcher.read { readSettings() }

    override suspend fun set(settings: AppSettings): DomainResult<Unit> = ioDispatcher.write {
        val result = runCatchingStorage {
            driver.transaction {
                settings.toEntries().forEach { (key, value) ->
                    driver.execute(
                        sql = """
                            INSERT INTO ${DatabaseSchema.Settings.TABLE}
                                (${DatabaseSchema.Settings.KEY}, ${DatabaseSchema.Settings.VALUE})
                            VALUES (?, ?)
                            ON CONFLICT(${DatabaseSchema.Settings.KEY}) DO UPDATE SET
                                ${DatabaseSchema.Settings.VALUE} = excluded.${DatabaseSchema.Settings.VALUE}
                        """.trimIndent(),
                        arguments = listOf(SqlValue.Text(key), SqlValue.Text(value)),
                    )
                }
            }
            Unit
        }
        if (result is DomainResult.Success) changes.notifyChanged()
        result
    }

    private fun readSettings(): AppSettings {
        val defaults = AppSettings.Default
        val stored = mutableMapOf<String, String>()
        driver.selectRows(
            sql = "SELECT ${DatabaseSchema.Settings.KEY}, ${DatabaseSchema.Settings.VALUE} FROM ${DatabaseSchema.Settings.TABLE}",
            transform = { cursor ->
                cursor.getString(0)?.let { key -> cursor.getString(1)?.let { value -> stored[key] = value } }
            },
        )

        return AppSettings(
            defaultMeterId = stored[KEY_DEFAULT_METER_ID],
            appearance = stored[KEY_APPEARANCE]
                ?.let { value -> AppearanceMode.entries.firstOrNull { it.name == value } }
                ?: defaults.appearance,
            unitSystem = stored[KEY_UNIT_SYSTEM]
                ?.let { value -> UnitSystem.entries.firstOrNull { it.name == value } }
                ?: defaults.unitSystem,
            language = stored[KEY_LANGUAGE]
                ?.let { value -> AppLanguage.entries.firstOrNull { it.name == value } }
                ?: defaults.language,
            tokenFormatProfileId = stored[KEY_TOKEN_FORMAT_PROFILE] ?: defaults.tokenFormatProfileId,
            allowConnectionAttempts = stored[KEY_ALLOW_CONNECTION_ATTEMPTS]?.toBooleanStrictOrNull()
                ?: defaults.allowConnectionAttempts,
            developerMode = stored[KEY_DEVELOPER_MODE]?.toBooleanStrictOrNull() ?: defaults.developerMode,
        )
    }

    private fun AppSettings.toEntries(): List<Pair<String, String>> = buildList {
        defaultMeterId?.let { add(KEY_DEFAULT_METER_ID to it) }
        add(KEY_APPEARANCE to appearance.name)
        add(KEY_UNIT_SYSTEM to unitSystem.name)
        add(KEY_LANGUAGE to language.name)
        add(KEY_TOKEN_FORMAT_PROFILE to tokenFormatProfileId)
        add(KEY_ALLOW_CONNECTION_ATTEMPTS to allowConnectionAttempts.toString())
        add(KEY_DEVELOPER_MODE to developerMode.toString())
    }

    companion object {
        const val KEY_DEFAULT_METER_ID = "default_meter_id"
        const val KEY_APPEARANCE = "appearance"
        const val KEY_UNIT_SYSTEM = "unit_system"
        const val KEY_LANGUAGE = "language"
        const val KEY_TOKEN_FORMAT_PROFILE = "token_format_profile_id"
        const val KEY_ALLOW_CONNECTION_ATTEMPTS = "allow_connection_attempts"
        const val KEY_DEVELOPER_MODE = "developer_mode"

        /** Settings keys the application writes. Used by tests and diagnostics. */
        val ALL_KEYS: List<String> = listOf(
            KEY_DEFAULT_METER_ID,
            KEY_APPEARANCE,
            KEY_UNIT_SYSTEM,
            KEY_LANGUAGE,
            KEY_TOKEN_FORMAT_PROFILE,
            KEY_ALLOW_CONNECTION_ATTEMPTS,
            KEY_DEVELOPER_MODE,
        )
    }
}

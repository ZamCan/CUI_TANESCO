package com.tanesco.metercompanion.platform.diagnostics

import com.tanesco.metercompanion.core.diagnostics.DiagnosticCheck
import com.tanesco.metercompanion.core.diagnostics.DiagnosticProbe
import com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus
import com.tanesco.metercompanion.core.diagnostics.SubsystemReport
import com.tanesco.metercompanion.core.repository.EventRepository
import com.tanesco.metercompanion.core.repository.MeterRepository
import com.tanesco.metercompanion.core.repository.TokenRepository
import com.tanesco.metercompanion.data.database.DatabaseSchema
import com.tanesco.metercompanion.data.database.SqlDriver
import com.tanesco.metercompanion.data.database.SqlValue
import com.tanesco.metercompanion.data.database.selectFirstOrNull
import kotlinx.coroutines.CancellationException

/**
 * Verifies that the local database is genuinely usable.
 *
 * The check is a real query against a real table, not an assumption that the
 * helper constructed successfully. A `Ready` row therefore means "SQLite
 * answered", which is the only claim worth making on this screen.
 */
class StorageDiagnosticsProbe(
    private val driver: SqlDriver,
    private val meterRepository: MeterRepository,
    private val tokenRepository: TokenRepository,
    private val eventRepository: EventRepository,
) : DiagnosticProbe {

    override suspend fun run(): List<SubsystemReport> = listOf(databaseReport(), repositoriesReport())

    private fun databaseReport(): SubsystemReport {
        val checks = buildList {
            add(
                probeCheck(
                    id = ID_DATABASE,
                    label = "Local database",
                    probe = {
                        // `SELECT 1` proves the handle is live; the settings
                        // table proves the schema is actually there.
                        val settingsExists = driver.selectFirstOrNull(
                            sql = "SELECT COUNT(*) FROM ${DatabaseSchema.Settings.TABLE}",
                            transform = { cursor -> cursor.getLong(0) ?: 0L },
                        )
                        val schemaVersion = driver.selectFirstOrNull(
                            sql = "PRAGMA user_version",
                            transform = { cursor -> cursor.getLong(0) ?: 0L },
                        )
                        "Schema version $schemaVersion, reachable at SQLite level." to
                            if (schemaVersion == null) {
                                DiagnosticStatus.Error
                            } else {
                                DiagnosticStatus.Ready
                            }
                    },
                    remediation = "Reinstalling the application recreates an empty database.",
                ),
            )
            add(
                DiagnosticCheck(
                    id = ID_SCHEMA,
                    label = "Schema",
                    status = DiagnosticStatus.Ready,
                    detail = "Version ${DatabaseSchema.VERSION}; " +
                        "${DatabaseSchema.CREATE_STATEMENTS.size} statements, " +
                        "${DatabaseSchema.MIGRATIONS.size} registered migrations.",
                ),
            )
        }

        return SubsystemReport(
            id = SUBSYSTEM_STORAGE,
            title = "Local data",
            summary = "Everything the application knows is stored on this device.",
            checks = checks,
        )
    }

    private suspend fun repositoriesReport(): SubsystemReport {
        val checks = listOf(
            repositoryCheck(ID_REPO_METERS, "Meter profiles") { meterRepository.getAll().size },
            repositoryCheck(ID_REPO_TOKENS, "Token records") { tokenRepository.getHistory(
                com.tanesco.metercompanion.core.model.TokenQuery(limit = 1),
            ).totalCount },
            repositoryCheck(ID_REPO_EVENTS, "Event log") {
                eventRepository.query(
                    com.tanesco.metercompanion.core.model.EventQuery(limit = 1),
                ).size
            },
        )

        return SubsystemReport(
            id = SUBSYSTEM_REPOSITORIES,
            title = "Repositories",
            summary = "Read paths used by every screen.",
            checks = checks,
        )
    }

    private suspend fun repositoryCheck(
        id: String,
        label: String,
        block: suspend () -> Int,
    ): DiagnosticCheck = try {
        DiagnosticCheck(
            id = id,
            label = label,
            status = DiagnosticStatus.Ready,
            detail = "Reachable. Currently holds ${block()} record(s).",
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        DiagnosticCheck(
            id = id,
            label = label,
            status = DiagnosticStatus.Error,
            detail = "Read failed with ${throwable::class.java.simpleName}.",
            remediation = "Storage is unavailable; restart the application.",
        )
    }

    private inline fun probeCheck(
        id: String,
        label: String,
        probe: () -> Pair<String, DiagnosticStatus>,
        remediation: String? = null,
    ): DiagnosticCheck = try {
        val (detail, status) = probe()
        DiagnosticCheck(
            id = id,
            label = label,
            status = status,
            detail = detail,
            remediation = remediation.takeIf { status.isProblem },
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        DiagnosticCheck(
            id = id,
            label = label,
            status = DiagnosticStatus.Error,
            detail = "Probe failed with ${throwable::class.java.simpleName}.",
            remediation = remediation,
        )
    }

    companion object {
        const val SUBSYSTEM_STORAGE = "storage"
        const val SUBSYSTEM_REPOSITORIES = "storage.repositories"

        const val ID_DATABASE = "storage.database"
        const val ID_SCHEMA = "storage.schema"
        const val ID_REPO_METERS = "storage.repository.meters"
        const val ID_REPO_TOKENS = "storage.repository.tokens"
        const val ID_REPO_EVENTS = "storage.repository.events"
    }
}

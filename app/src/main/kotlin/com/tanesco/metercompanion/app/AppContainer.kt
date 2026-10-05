package com.tanesco.metercompanion.app

import android.content.Context
import com.tanesco.metercompanion.app.platform.AppStartupCoordinator
import com.tanesco.metercompanion.connection.ConnectionCoordinator
import com.tanesco.metercompanion.connection.ConnectionDiagnosticsProbe
import com.tanesco.metercompanion.connection.SingleAdapterMeterConnectionProvider
import com.tanesco.metercompanion.core.diagnostics.DiagnosticsService
import com.tanesco.metercompanion.core.diagnostics.DomainDiagnosticsProbe
import com.tanesco.metercompanion.core.history.EventRecorder
import com.tanesco.metercompanion.core.meter.MeterProfileService
import com.tanesco.metercompanion.core.meter.MeterProfileValidator
import com.tanesco.metercompanion.core.model.EventCategory
import com.tanesco.metercompanion.core.model.EventNames
import com.tanesco.metercompanion.core.repository.EventRepository
import com.tanesco.metercompanion.core.repository.MeterRepository
import com.tanesco.metercompanion.core.repository.MeterSessionRepository
import com.tanesco.metercompanion.core.repository.SettingsRepository
import com.tanesco.metercompanion.core.repository.TokenRepository
import com.tanesco.metercompanion.core.time.Clock
import com.tanesco.metercompanion.core.time.IdGenerator
import com.tanesco.metercompanion.core.token.TokenFormatProfileRegistry
import com.tanesco.metercompanion.core.token.TokenService
import com.tanesco.metercompanion.core.token.TokenValidationEngine
import com.tanesco.metercompanion.data.repository.SqlEventRepository
import com.tanesco.metercompanion.data.repository.SqlMeterRepository
import com.tanesco.metercompanion.data.repository.SqlMeterSessionRepository
import com.tanesco.metercompanion.data.repository.SqlSettingsRepository
import com.tanesco.metercompanion.data.repository.SqlTokenRepository
import com.tanesco.metercompanion.platform.AndroidAppMetadataProvider
import com.tanesco.metercompanion.platform.AndroidDispatchers
import com.tanesco.metercompanion.platform.database.AndroidSqlDriver
import com.tanesco.metercompanion.platform.diagnostics.StorageDiagnosticsProbe

/**
 * The application's dependency graph, assembled by hand.
 *
 * Why not a DI framework
 * ----------------------
 * Hilt or Dagger would add an annotation processor to a build that already runs
 * on a phone, roughly doubling configuration time for a graph this small. Manual
 * construction is also *more* explicit about the one architectural rule that
 * matters most here: the concrete [com.tanesco.metercompanion.connection.MeterConnection]
 * is chosen in exactly one place, this file, and nothing else in the codebase
 * names a concrete adapter.
 *
 * Dependencies are declared `by lazy` so that constructing the container is
 * cheap; nothing touches the database until a screen actually needs data.
 *
 * See docs/decisions/0004-dependency-injection.md.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    // --- Platform ----------------------------------------------------------

    val dispatchers: AndroidDispatchers by lazy { AndroidDispatchers.create() }

    private val sqlDriver: AndroidSqlDriver by lazy { AndroidSqlDriver(appContext) }

    val appMetadataProvider: AndroidAppMetadataProvider by lazy {
        AndroidAppMetadataProvider(appContext)
    }

    // --- Time and identifiers ---------------------------------------------

    val clock: Clock = Clock.System

    val idGenerator: IdGenerator = IdGenerator.Random

    // --- Repositories ------------------------------------------------------

    val meterRepository: MeterRepository by lazy {
        SqlMeterRepository(sqlDriver, dispatchers.io)
    }

    val tokenRepository: TokenRepository by lazy {
        SqlTokenRepository(sqlDriver, dispatchers.io)
    }

    val eventRepository: EventRepository by lazy {
        SqlEventRepository(sqlDriver, dispatchers.io)
    }

    val settingsRepository: SettingsRepository by lazy {
        SqlSettingsRepository(sqlDriver, dispatchers.io)
    }

    val meterSessionRepository: MeterSessionRepository by lazy {
        SqlMeterSessionRepository(sqlDriver, dispatchers.io)
    }

    // --- Domain services ---------------------------------------------------

    val eventRecorder: EventRecorder by lazy {
        EventRecorder(eventRepository, clock, idGenerator)
    }

    val meterProfileService: MeterProfileService by lazy {
        MeterProfileService(
            meterRepository = meterRepository,
            settingsRepository = settingsRepository,
            validator = MeterProfileValidator(),
            events = eventRecorder,
            clock = clock,
            idGenerator = idGenerator,
        )
    }

    private val tokenProfiles: TokenFormatProfileRegistry by lazy {
        TokenFormatProfileRegistry()
    }

    val tokenService: TokenService by lazy {
        TokenService(
            tokenRepository = tokenRepository,
            settingsRepository = settingsRepository,
            validationEngine = TokenValidationEngine(tokenProfiles, clock),
            profiles = tokenProfiles,
            events = eventRecorder,
            clock = clock,
            idGenerator = idGenerator,
        )
    }

    // --- Connection boundary ----------------------------------------------
    //
    // The ONLY place in the application that names a concrete adapter. Swapping
    // in a real, authorised implementation later is a change to this function
    // and to nothing else.

    val connectionProvider: SingleAdapterMeterConnectionProvider by lazy {
        SingleAdapterMeterConnectionProvider.withResearchAdapter()
    }

    val connectionCoordinator: ConnectionCoordinator by lazy {
        ConnectionCoordinator(
            provider = connectionProvider,
            sessionRepository = meterSessionRepository,
            eventRepository = eventRepository,
            clock = clock,
            idGenerator = idGenerator,
            scope = kotlinx.coroutines.CoroutineScope(dispatchers.computation),
        )
    }

    // --- Diagnostics -------------------------------------------------------

    val diagnosticsService: DiagnosticsService by lazy {
        DiagnosticsService(
            probes = listOf(
                DomainDiagnosticsProbe(
                    tokenEngine = TokenValidationEngine(tokenProfiles, clock),
                    profiles = tokenProfiles,
                    eventRepository = eventRepository,
                ),
                StorageDiagnosticsProbe(
                    driver = sqlDriver,
                    meterRepository = meterRepository,
                    tokenRepository = tokenRepository,
                    eventRepository = eventRepository,
                ),
                ConnectionDiagnosticsProbe(
                    provider = connectionProvider,
                    coordinator = connectionCoordinator,
                ),
            ),
            clock = clock,
        )
    }

    /**
     * Runs one-time startup work: opens the database, records that the
     * application started, and begins mirroring adapter state.
     */
    suspend fun onApplicationStarted() {
        AppStartupCoordinator(
            eventRecorder = eventRecorder,
            connectionCoordinator = connectionCoordinator,
        ).run()
    }

    /** Releases the database handle and the dispatcher thread. */
    fun close() {
        dispatchers.shutdown()
    }
}

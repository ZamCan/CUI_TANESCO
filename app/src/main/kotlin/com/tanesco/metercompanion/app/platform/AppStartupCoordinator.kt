package com.tanesco.metercompanion.app.platform

import com.tanesco.metercompanion.connection.ConnectionCoordinator
import com.tanesco.metercompanion.core.history.EventRecorder
import com.tanesco.metercompanion.core.model.EventCategory
import com.tanesco.metercompanion.core.model.EventNames
import com.tanesco.metercompanion.core.model.EventSeverity
import kotlinx.coroutines.cancellation.CancellationException

/**
 * One-time work performed when the application starts.
 *
 * Kept out of the [android.app.Application] class so that it can be unit tested
 * with fakes and so the activity does not have to know what "starting up"
 * involves.
 */
class AppStartupCoordinator(
    private val eventRecorder: EventRecorder,
    private val connectionCoordinator: ConnectionCoordinator,
) {

    /**
     * Records the startup event and begins mirroring adapter state.
     *
     * Failure here is deliberately non-fatal: an unwritable event log must not
     * stop the application from launching. The condition remains visible on the
     * Diagnostics screen, which reports log health independently.
     */
    suspend fun run() {
        connectionCoordinator.bindAdapter()

        try {
            eventRecorder.record(
                category = EventCategory.System,
                name = EventNames.STARTUP_COMPLETED,
                severity = EventSeverity.Info,
                summary = "Application started",
                detail = "connection adapter '${connectionCoordinator.capabilities()?.id ?: "none"}'",
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            // Intentionally swallowed; see the doc comment above.
        }
    }
}

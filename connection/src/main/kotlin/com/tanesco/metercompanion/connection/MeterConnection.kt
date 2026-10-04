package com.tanesco.metercompanion.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The meter connection boundary.
 *
 * This is the single seam between the application and any physical meter. Every
 * screen, service and repository above it depends on this interface alone; no
 * concrete adapter is referenced anywhere else in the codebase.
 *
 * ## Contract for implementors
 *
 * An implementation must:
 *
 *  1. Report its true [ConnectionCapabilities], including what it *cannot* do.
 *  2. Return a failure - never a fabricated success - when an operation cannot
 *     be completed.
 *  3. Never mutate meter state in a way the meter did not confirm.
 *  4. Be safe to call from any coroutine context and must not block the caller
 *     while holding a lock the UI might need.
 *
 * ## Deliberately out of scope
 *
 * No implementation of undocumented CIU or meter protocols belongs here. Only
 * behaviour backed by an authoritative specification and lawful authorisation
 * may be added. See docs/connection/CONNECTION-BOUNDARY.md.
 *
 * Every operation suspends and returns a [ConnectionResult], so callers are
 * forced to handle failure. There are no functions that can silently do nothing.
 */
interface MeterConnection {

    /** Stable identifier of this adapter, e.g. `research-placeholder`. */
    val id: String

    /**
     * What this adapter can do.
     *
     * Non-suspending because capabilities are static per adapter: an
     * implementation that gains or loses capability at runtime should publish a
     * new adapter instance instead of mutating this value.
     */
    fun getCapabilities(): ConnectionCapabilities

    /** The current state, readable synchronously for first composition. */
    fun getConnectionState(): ConnectionState

    /** The same state as a stream, for reactive UI. */
    fun observeState(): StateFlow<ConnectionState>

    /**
     * Attempts to open a session with a meter.
     *
     * Returns [ConnectionFailure.ResearchPending] when the capability is not
     * implemented yet, and
     * [ConnectionFailure.UnsupportedOperation] when this specific adapter does
     * not offer the requested capability.
     */
    suspend fun connect(request: ConnectionRequest): ConnectionResult<ConnectionSession>

    /** Closes any open session. Safe to call when already disconnected. */
    suspend fun disconnect(): ConnectionResult<Unit>

    /**
     * Reads whatever the meter chooses to report.
     *
     * A correct implementation returns [ConnectionFailure.ResearchPending] today.
     * It must never return a [MeterSnapshot] containing values that did not come
     * from a meter.
     */
    suspend fun readMeterState(): ConnectionResult<MeterSnapshot>
}

/**
 * Holds the adapter the application should use.
 *
 * Exists so that the application depends on an *abstraction of adapters*
 * rather than on a registry that hands out a concrete instance. Registering an
 * adapter is the only supported way to change behaviour; no screen ever
 * constructs one directly.
 */
interface MeterConnectionProvider {

    /**
     * The adapter currently in use, or `null` when none has been registered.
     *
     * `null` is a legitimate, fully supported state: the application must render
     * "Connection unavailable" rather than treat this as an error.
     */
    fun current(): MeterConnection?

    /** Registers [connection] as the active adapter. */
    fun register(connection: MeterConnection)

    /** Capabilities of every registered adapter, for the Diagnostics screen. */
    fun registeredCapabilities(): List<ConnectionCapabilities>
}

/**
 * A [MeterConnectionProvider] backed by a single mutable slot.
 *
 * Thread safety: [register] and [current] may be called from any thread; access
 * is guarded by a monitor. State *changes* are published through the adapter's
 * own [StateFlow], so consumers always observe changes on their own scope.
 */
class SingleAdapterMeterConnectionProvider : MeterConnectionProvider {

    private val lock = Any()

    @Volatile
    private var connection: MeterConnection? = null

    override fun current(): MeterConnection? = connection

    override fun register(connection: MeterConnection) {
        synchronized(lock) {
            this.connection = connection
        }
    }

    override fun registeredCapabilities(): List<ConnectionCapabilities> =
        current()?.let { listOf(it.getCapabilities()) }.orEmpty()

    companion object {
        /**
         * Provider pre-loaded with [ResearchMeterConnection].
         *
         * Used by the application so that the connection boundary is always
         * reachable, while still reporting an honest research state.
         */
        fun withResearchAdapter(): SingleAdapterMeterConnectionProvider =
            SingleAdapterMeterConnectionProvider().apply {
                register(ResearchMeterConnection())
            }
    }
}

/** Observes the state stream of [this], for callers that only need changes. */
fun MeterConnection.states(): Flow<ConnectionState> = observeState()

package com.tanesco.metercompanion.core.model

/**
 * Stable event names written to the local log.
 *
 * These strings are persisted, so they are part of the data contract: renaming
 * one is a migration concern. They are used by tests and by the diagnostics
 * screen's counters, which is why they live in one place.
 */
object EventNames {
    const val METER_PROFILE_CREATED = "meter.profile_created"
    const val METER_PROFILE_UPDATED = "meter.profile_updated"
    const val METER_PROFILE_ARCHIVED = "meter.profile_archived"
    const val METER_PROFILE_REJECTED = "meter.profile_rejected"
    const val METER_DEFAULT_CHANGED = "meter.default_changed"

    const val TOKEN_ENTERED = "token.entered"
    const val TOKEN_NORMALIZED = "token.normalized"
    const val TOKEN_VALIDATION_PASSED = "token.validation_passed"
    const val TOKEN_VALIDATION_FAILED = "token.validation_failed"
    const val TOKEN_DUPLICATE_DETECTED = "token.duplicate_detected"
    const val TOKEN_STORED = "token.stored"

    const val CONNECTION_ATTEMPTED = "connection.attempted"
    const val CONNECTION_UNAVAILABLE = "connection.unavailable"
    const val CONNECTION_RESEARCH_PENDING = "connection.research_pending"
    const val CONNECTION_UNSUPPORTED = "connection.unsupported_operation"
    const val CONNECTION_DENIED = "connection.permission_denied"
    const val CONNECTION_FAILED = "connection.failed"
    const val CONNECTION_SESSION_CLOSED = "connection.session_closed"
    const val CONNECTION_READ_REQUESTED = "connection.read_requested"
    const val CONNECTION_DISCONNECTED = "connection.disconnected"

    const val SETTINGS_CHANGED = "settings.changed"
    const val STORAGE_MAINTENANCE = "storage.maintenance"
    const val STORAGE_ERROR = "storage.error"
    const val DIAGNOSTICS_GENERATED = "diagnostics.generated"
    const val STARTUP_COMPLETED = "system.startup_completed"
}

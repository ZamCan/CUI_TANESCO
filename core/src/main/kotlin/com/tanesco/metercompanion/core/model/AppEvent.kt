package com.tanesco.metercompanion.core.model

import com.tanesco.metercompanion.core.time.Clock

/**
 * Severity of a recorded [AppEvent].
 *
 * Ordering matters: it is used both for filtering and for choosing the visual
 * treatment in Activity.
 */
enum class EventSeverity(val label: String) {
    Info("Info"),
    Success("Success"),
    Warning("Warning"),
    Error("Error"),
}

/**
 * Categories of application activity.
 *
 * These are stable identifiers. Renaming one is a data migration concern
 * because persisted events keep the name.
 */
enum class EventCategory(val label: String) {
    Meter("Meter profile"),
    Token("Token"),
    Connection("Connection"),
    Diagnostics("Diagnostics"),
    Settings("Settings"),
    System("Application"),
}

/**
 * An immutable record of something that happened in the application.
 *
 * The event log is append-only and is the reason the Activity and Diagnostics
 * screens can be trusted: every statement those screens make is backed by a row
 * in this log rather than by transient UI state.
 */
data class AppEvent(
    val id: String,
    val category: EventCategory,
    /** Stable event name, e.g. `token.validation_failed`. */
    val name: String,
    val severity: EventSeverity,
    val occurredAtMillis: Long,
    /** Short, human readable summary. Already safe to display. */
    val summary: String,
    /** Optional longer technical context. */
    val detail: String? = null,
    val meterId: String? = null,
    /** True when the user performed this action rather than the app. */
    val userInitiated: Boolean = false,
) {
    companion object {
        /**
         * Builds an event with a generated id and the current time.
         *
         * Centralising construction keeps every event consistent and guarantees
         * that a caller cannot forget a timestamp.
         */
        fun create(
            id: String,
            category: EventCategory,
            name: String,
            severity: EventSeverity,
            summary: String,
            clock: Clock,
            detail: String? = null,
            meterId: String? = null,
            userInitiated: Boolean = false,
        ): AppEvent = AppEvent(
            id = id,
            category = category,
            name = name,
            severity = severity,
            occurredAtMillis = clock.nowMillis(),
            summary = summary,
            detail = detail,
            meterId = meterId,
            userInitiated = userInitiated,
        )
    }
}

/**
 * A filter over the event log, applied by the repository so that paging can
 * stay cheap.
 */
data class EventQuery(
    val category: EventCategory? = null,
    val meterId: String? = null,
    val limit: Int = 200,
    /** Only events at or after this instant. */
    val sinceMillis: Long? = null,
) {
    init {
        require(limit > 0) { "limit must be positive" }
    }
}

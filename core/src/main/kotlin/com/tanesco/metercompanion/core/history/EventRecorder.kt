package com.tanesco.metercompanion.core.history

import com.tanesco.metercompanion.core.model.AppEvent
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.EventCategory
import com.tanesco.metercompanion.core.model.EventSeverity
import com.tanesco.metercompanion.core.repository.EventRepository
import com.tanesco.metercompanion.core.time.Clock
import com.tanesco.metercompanion.core.time.IdGenerator

/**
 * Single entry point for writing to the activity log.
 *
 * Services never construct an [AppEvent] directly: they call this recorder so
 * that every event has a generated id, a timestamp from the injected clock, and
 * a consistent shape.
 *
 * Logging is intentionally best-effort with respect to *business* outcomes. A
 * failure to write the log must never turn a successful operation into a failed
 * one, so callers get the [DomainResult] of the log write but are expected to
 * treat it as secondary. A log failure is surfaced through Diagnostics rather
 * than by blocking the user.
 */
class EventRecorder(
    private val eventRepository: EventRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {

    suspend fun record(
        category: EventCategory,
        name: String,
        severity: EventSeverity,
        summary: String,
        detail: String? = null,
        meterId: String? = null,
        userInitiated: Boolean = false,
    ): DomainResult<Unit> {
        val event = AppEvent.create(
            id = idGenerator.newId(),
            category = category,
            name = name,
            severity = severity,
            summary = summary,
            clock = clock,
            detail = detail,
            meterId = meterId,
            userInitiated = userInitiated,
        )
        return eventRepository.append(event)
    }

    suspend fun info(
        category: EventCategory,
        name: String,
        summary: String,
        detail: String? = null,
        meterId: String? = null,
        userInitiated: Boolean = false,
    ): DomainResult<Unit> = record(
        category, name, EventSeverity.Info, summary, detail, meterId, userInitiated,
    )

    suspend fun success(
        category: EventCategory,
        name: String,
        summary: String,
        detail: String? = null,
        meterId: String? = null,
        userInitiated: Boolean = false,
    ): DomainResult<Unit> = record(
        category, name, EventSeverity.Success, summary, detail, meterId, userInitiated,
    )

    suspend fun warning(
        category: EventCategory,
        name: String,
        summary: String,
        detail: String? = null,
        meterId: String? = null,
        userInitiated: Boolean = false,
    ): DomainResult<Unit> = record(
        category, name, EventSeverity.Warning, summary, detail, meterId, userInitiated,
    )

    suspend fun error(
        category: EventCategory,
        name: String,
        summary: String,
        detail: String? = null,
        meterId: String? = null,
        userInitiated: Boolean = false,
    ): DomainResult<Unit> = record(
        category, name, EventSeverity.Error, summary, detail, meterId, userInitiated,
    )
}

package com.tanesco.metercompanion.app.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanesco.metercompanion.core.model.AppEvent
import com.tanesco.metercompanion.core.model.EventCategory
import com.tanesco.metercompanion.core.model.EventQuery
import com.tanesco.metercompanion.core.repository.EventRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Everything the Activity screen renders. */
data class ActivityUiState(
    val events: List<AppEvent> = emptyList(),
    val activeFilter: ActivityFilter = ActivityFilter.All,
    val isLoading: Boolean = true,
)

/** Filter chips shown above the activity list. */
enum class ActivityFilter(val label: String, val category: EventCategory?) {
    All("All", null),
    Meter("Meters", EventCategory.Meter),
    Token("Tokens", EventCategory.Token),
    Connection("Connection", EventCategory.Connection),
    Diagnostics("Diagnostics", EventCategory.Diagnostics),
    Settings("Settings", EventCategory.Settings),
}

/**
 * Drives the Activity screen.
 *
 * The log is the application's audit trail, so it is read straight from the
 * repository with no caching or client-side embellishment: everything shown is
 * exactly what was written. Changing the filter re-queries rather than
 * filtering in memory, so the ordering and limit stay the repository's
 * responsibility.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel(
    private val eventRepository: EventRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(ActivityFilter.All)

    private val events = filter.flatMapLatest { active ->
        eventRepository.observe(EventQuery(category = active.category, limit = PAGE_SIZE))
    }

    val state: StateFlow<ActivityUiState> = combine(filter, events) { active, log ->
        ActivityUiState(
            events = log,
            activeFilter = active,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = ActivityUiState(),
    )

    fun selectFilter(value: ActivityFilter) {
        filter.value = value
    }

    /**
     * Deletes events older than [olderThanMillis] and returns how many.
     *
     * Settings uses this for local-data maintenance. Returning the count lets
     * the UI report exactly what was removed rather than claiming a generic
     * success.
     */
    suspend fun pruneOlderThan(olderThanMillis: Long): Int = eventRepository.prune(olderThanMillis)

    private companion object {
        const val PAGE_SIZE = 200
    }
}

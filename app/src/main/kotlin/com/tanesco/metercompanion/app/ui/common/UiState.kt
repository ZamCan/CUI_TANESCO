package com.tanesco.metercompanion.app.ui.common

import com.tanesco.metercompanion.core.model.DomainError

/**
 * The state of a screen that loads one thing.
 *
 * [Initial] is separate from [Loading] on purpose: a screen that has not started
 * loading should show neither a spinner nor an error, and collapsing the two
 * makes it impossible to tell "nothing has happened yet" from "we are working".
 *
 * Empty data is represented *inside* [Ready] rather than as its own state,
 * because "loaded successfully and there is nothing here" is a normal outcome
 * that each screen renders with its own empty state.
 */
sealed interface UiState<out T> {

    /** Nothing has been requested yet. */
    data object Initial : UiState<Nothing>

    /** A request is in flight and there is nothing to show. */
    data object Loading : UiState<Nothing>

    /** Data is available. May legitimately be empty. */
    data class Ready<out T>(val data: T) : UiState<T>

    /** The request failed. [error] carries a safe, user-facing message. */
    data class Failed(val error: DomainError) : UiState<Nothing>
}

/** Convenience accessors used by screens. */
val UiState<*>.isLoading: Boolean get() = this is UiState.Loading

val UiState<*>.isInitial: Boolean get() = this is UiState.Initial

/** Data if ready, otherwise `null`. */
fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Ready<T>)?.data

/**
 * A short, safe description of [DomainError] for display.
 *
 * Copy is derived from the error's *code*, never from a platform exception
 * message, so nothing sensitive can reach the screen by accident.
 */
fun DomainError.userMessage(): String = when (this) {
    is DomainError.Validation -> detail
    is DomainError.NotFound -> "That item is no longer available."
    is DomainError.Conflict -> detail
    is DomainError.Storage -> "Local storage could not be updated. Try again."
    is DomainError.Connection -> detail
    is DomainError.Unavailable -> detail
    is DomainError.Unexpected -> "Something went wrong. The details are in Diagnostics."
}

/** Stable code, for logging and for tests. */
val DomainError.debugCode: String get() = code

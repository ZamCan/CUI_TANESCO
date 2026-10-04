package com.tanesco.metercompanion.connection

import com.tanesco.metercompanion.core.model.DomainError
import com.tanesco.metercompanion.core.model.DomainResult

/**
 * Result of a connection operation.
 *
 * Separate from [DomainResult] on purpose: the connection layer describes
 * transport-shaped failures, and [toDomainError] performs the single, explicit
 * translation into the vocabulary the rest of the application uses.
 */
sealed interface ConnectionResult<out T> {

    data class Success<out T>(val value: T) : ConnectionResult<T>

    data class Failure(val failure: ConnectionFailure) : ConnectionResult<Nothing>

    val isSuccess: Boolean get() = this is Success

    fun <R> map(transform: (T) -> R): ConnectionResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    /** Maps a value to another result type, propagating failure unchanged. */
    fun <R> flatMap(transform: (T) -> ConnectionResult<R>): ConnectionResult<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    companion object {
        fun success(value: T): ConnectionResult<T> = Success(value)
    }
}

/**
 * Why a connection operation failed.
 *
 * Each variant maps to a distinct [DomainError] so that presentation can react
 * differently - a missing permission is actionable, an unimplemented
 * capability is not.
 */
sealed interface ConnectionFailure {

    /** Stable code used for persistence, logging and tests. */
    val code: String

    /** The capability this failure is about, when one applies. */
    val capability: String?

    /**
     * The adapter exists but cannot operate here: no hardware, missing
     * permission, absent transport.
     */
    data class Unavailable(
        override val code: String,
        override val capability: String?,
        val reason: String,
    ) : ConnectionFailure

    /**
     * Not implemented on purpose, pending technical research.
     *
     * This is the expected result for every meter operation at present. It must
     * be reported as an informational state, never as an error.
     */
    data class ResearchPending(
        override val code: String,
        override val capability: String?,
        val scope: String,
        val note: String,
    ) : ConnectionFailure

    /** The transport was engaged but the exchange failed. */
    data class TransportError(
        override val code: String,
        override val capability: String?,
        val reason: String,
        val cause: Throwable? = null,
    ) : ConnectionFailure

    /** The request was rejected before any transport work started. */
    data class InvalidRequest(
        override val code: String,
        val reason: String,
    ) : ConnectionFailure {
        override val capability: String? get() = null
    }

    /** A required runtime permission was not granted. */
    data class PermissionDenied(
        override val code: String,
        val permission: String,
    ) : ConnectionFailure {
        override val capability: String? get() = null
    }

    /**
     * The adapter does not implement [capability].
     *
     * This is how the UI learns to hide an action rather than let the user
     * trigger a guaranteed failure.
     */
    data class UnsupportedOperation(
        override val code: String,
        override val capability: String,
        val reason: String,
    ) : ConnectionFailure

    /** A defect in the adapter. Always investigated. */
    data class Internal(
        override val code: String,
        val reason: String,
        val cause: Throwable? = null,
    ) : ConnectionFailure {
        override val capability: String? get() = null
    }
}

/**
 * Translates a [ConnectionFailure] into the application's error vocabulary.
 *
 * This is the only place the two vocabularies meet, which keeps the connection
 * boundary independent of the domain's presentation-facing types.
 */
fun ConnectionFailure.toDomainError(): DomainError = when (this) {
    is ConnectionFailure.Unavailable -> DomainError.Unavailable(
        code = code,
        capability = capability ?: "meter connection",
        detail = reason,
    )

    is ConnectionFailure.ResearchPending -> DomainError.Unavailable(
        code = code,
        capability = capability ?: scope,
        detail = note,
    )

    is ConnectionFailure.TransportError -> DomainError.Connection(
        code = code,
        detail = reason,
        cause = cause,
    )

    is ConnectionFailure.InvalidRequest -> DomainError.Validation(
        code = code,
        detail = reason,
    )

    is ConnectionFailure.PermissionDenied -> DomainError.Connection(
        code = code,
        detail = "permission '$permission' was not granted",
    )

    is ConnectionFailure.UnsupportedOperation -> DomainError.Unavailable(
        code = code,
        capability = capability,
        detail = reason,
    )

    is ConnectionFailure.Internal -> DomainError.Unexpected(cause)
}

/** Convenience: lift a [ConnectionResult] into the domain's result type. */
fun <T> ConnectionResult<T>.toDomainResult(): DomainResult<T> = when (this) {
    is ConnectionResult.Success -> DomainResult.Success(value)
    is ConnectionResult.Failure -> DomainResult.Failure(failure.toDomainError())
}

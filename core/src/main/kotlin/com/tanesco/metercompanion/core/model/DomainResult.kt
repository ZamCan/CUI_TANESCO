package com.tanesco.metercompanion.core.model

/**
 * Outcome of a domain operation.
 *
 * The application models failures as values instead of exceptions so that every
 * screen is forced to consider the failure branch at compile time. This is the
 * single most important type in the domain layer: it is what makes "design the
 * error state, not only the happy path" enforceable rather than aspirational.
 */
sealed interface DomainResult<out T> {

    /** The operation completed and produced [value]. */
    data class Success<out T>(val value: T) : DomainResult<T>

    /** The operation did not complete; [error] explains why. */
    data class Failure(val error: DomainError) : DomainResult<Nothing>

    val isSuccess: Boolean get() = this is Success

    /** Returns the value, or `null` when this is a [Failure]. */
    fun getOrNull(): T? = (this as? Success)?.value

    /** Returns the value, or [fallback] when this is a [Failure]. */
    fun getOrElse(fallback: @UnsafeVariance T): T = getOrNull() ?: fallback

    /** Returns the error, or `null` when this is a [Success]. */
    fun errorOrNull(): DomainError? = (this as? Failure)?.error

    /** Transforms a successful value, leaving a failure untouched. */
    fun <R> map(transform: (T) -> R): DomainResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    /** Chains another fallible operation onto a successful value. */
    fun <R> flatMap(transform: (T) -> DomainResult<R>): DomainResult<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    /** Collapses both branches into a single value. */
    fun <R> fold(onSuccess: (T) -> R, onFailure: (DomainError) -> R): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(error)
    }

    companion object {
        /** Wraps [block], converting a thrown exception into a [Failure]. */
        inline fun <T> catching(
            errorMapper: (Throwable) -> DomainError = { DomainError.Unexpected(it) },
            block: () -> T,
        ): DomainResult<T> = try {
            Success(block())
        } catch (cancellation: kotlinx.coroutines.cancellation.CancellationException) {
            // Never swallow coroutine cancellation: it is control flow, not failure.
            throw cancellation
        } catch (throwable: Throwable) {
            Failure(errorMapper(throwable))
        }
    }
}

/** Convenience factory for a successful [DomainResult]. */
fun <T> successOf(value: T): DomainResult<T> = DomainResult.Success(value)

/** Convenience factory for a failed [DomainResult]. */
fun failureOf(error: DomainError): DomainResult<Nothing> = DomainResult.Failure(error)

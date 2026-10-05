package com.tanesco.metercompanion.core.model

/**
 * A failure that the domain layer can describe without leaking platform types.
 *
 * Two rules govern this type:
 *
 *  1. [code] is stable and machine readable. Presentation maps it to localised
 *     copy; it is never used as user-facing text.
 *  2. [cause] is retained for diagnostics only. The diagnostics screen shows a
 *     type name and never the message, because a platform exception message can
 *     contain data that should not be displayed or exported.
 */
sealed interface DomainError {

    /** Stable identifier, e.g. `token.invalid_length`. Safe to log and to map. */
    val code: String

    /** The originating throwable, when the failure came from lower down. */
    val cause: Throwable?
        get() = null

    /** Short technical description intended for developer surfaces. */
    val technicalDetail: String

    data class Validation(
        override val code: String,
        val field: String? = null,
        val detail: String,
        override val cause: Throwable? = null,
    ) : DomainError {
        // `this.field` is required: a bare `field` inside a property accessor
        // refers to the backing field, not to this constructor property.
        override val technicalDetail: String
            get() = if (this.field == null) detail else "${this.field}: $detail"
    }

    data class NotFound(
        override val code: String,
        val entity: String,
        val id: String,
    ) : DomainError {
        override val technicalDetail: String get() = "$entity '$id' was not found"
    }

    /** The request conflicts with existing state, e.g. a duplicate token. */
    data class Conflict(
        override val code: String,
        val entity: String,
        val detail: String,
    ) : DomainError {
        override val technicalDetail: String get() = "$entity conflict: $detail"
    }

    /** Local storage failed. */
    data class Storage(
        override val code: String,
        val detail: String,
        override val cause: Throwable? = null,
    ) : DomainError {
        override val technicalDetail: String get() = "storage: $detail"
    }

    /**
     * The meter connection boundary refused or could not serve a request.
     *
     * [code] is expected to be one of the connection layer's stable codes, for
     * example `connection.adapter_unavailable`.
     */
    data class Connection(
        override val code: String,
        val detail: String,
        override val cause: Throwable? = null,
    ) : DomainError {
        override val technicalDetail: String get() = "connection: $detail"
    }

    /** No capability exists yet, by deliberate design rather than by fault. */
    data class Unavailable(
        override val code: String,
        val capability: String,
        val detail: String,
    ) : DomainError {
        override val technicalDetail: String get() = "$capability unavailable: $detail"
    }

    /** A defect. Always investigated, never shown as a normal outcome. */
    data class Unexpected(
        override val cause: Throwable?,
    ) : DomainError {
        override val code: String get() = "error.unexpected"
        override val technicalDetail: String
            get() = cause?.let { "${it::class.java.simpleName}" } ?: "unknown"
    }
}

package com.tanesco.metercompanion.core.validation

/**
 * A problem with one named input field.
 *
 * [message] is user-facing copy that is safe to display directly next to the
 * field. Keeping presentation copy in the domain is a deliberate trade: these
 * messages are short, factual and identical on every platform, so a single
 * wording avoids drift between Android and a future iOS build.
 */
data class FieldError(val field: String, val message: String)

/**
 * Accumulated field errors from validating a form.
 *
 * Validation always inspects *every* field rather than stopping at the first
 * failure, so the user sees all problems at once.
 */
class ValidationResult private constructor(
    private val errors: List<FieldError>,
) {
    val isValid: Boolean get() = errors.isEmpty()

    /** All errors, in the order they were reported. */
    fun all(): List<FieldError> = errors

    /** Errors for a single field, if any. */
    fun errorsFor(field: String): List<FieldError> = errors.filter { it.field == field }

    /** First message for [field], or `null` when the field is fine. */
    fun messageFor(field: String): String? = errorsFor(field).firstOrNull()?.message

    /** Every message for [field] joined for use as a single accessibility hint. */
    fun combinedMessageFor(field: String): String? =
        errorsFor(field).takeIf { it.isNotEmpty() }?.joinToString(" ") { it.message }

    fun withField(field: String, message: String): ValidationResult =
        ValidationResult(errors + FieldError(field, message))

    companion object {
        val Valid: ValidationResult = ValidationResult(emptyList())

        fun of(vararg errors: FieldError): ValidationResult =
            if (errors.isEmpty()) Valid else ValidationResult(errors.toList())
    }
}

/** Field names used across the application. Centralised to avoid typos. */
object Fields {
    const val ACCOUNT_NUMBER = "accountNumber"
    const val DISPLAY_NAME = "displayName"
    const val REGION = "region"
    const val NOTES = "notes"
    const val TOKEN = "token"
}

/**
 * Small, reusable predicates for input validation.
 *
 * Deliberately free of Android types so both platforms share one definition of
 * "valid".
 */
object Validators {

    private val ACCOUNT_NUMBER_ALLOWED = ('0'..'9').toSet() + setOf('-', ' ')

    /**
     * Validates an account or meter number.
     *
     * Accepts digits with optional spaces and hyphens. The minimum length is
     * conservative so that shorter numbering schemes are not rejected; the
     * exact per-utility scheme is configurable in future iterations.
     */
    fun accountNumber(raw: String): String? {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> "Enter the account or meter number."
            trimmed.any { it !in ACCOUNT_NUMBER_ALLOWED } ->
                "Use digits only; spaces and hyphens are allowed."

            trimmed.count { it.isDigit() } < MIN_ACCOUNT_NUMBER_DIGITS ->
                "The account number looks too short."

            trimmed.length > MAX_ACCOUNT_NUMBER_LENGTH ->
                "The account number is too long."

            else -> null
        }
    }

    /** Validates a customer-chosen display name. */
    fun displayName(raw: String): String? {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> "Enter a name for this meter."
            trimmed.length < MIN_DISPLAY_NAME_LENGTH ->
                "Use at least $MIN_DISPLAY_NAME_LENGTH characters."

            trimmed.length > MAX_DISPLAY_NAME_LENGTH ->
                "Use at most $MAX_DISPLAY_NAME_LENGTH characters."

            else -> null
        }
    }

    /** Validates an optional free-text note. An empty note is valid. */
    fun optionalNote(raw: String): String? {
        val trimmed = raw.trim()
        return if (trimmed.length > MAX_NOTE_LENGTH) {
            "Use at most $MAX_NOTE_LENGTH characters."
        } else {
            null
        }
    }

    /** Validates an optional region. An empty region is valid. */
    fun optionalRegion(raw: String): String? {
        val trimmed = raw.trim()
        return if (trimmed.length > MAX_REGION_LENGTH) {
            "Use at most $MAX_REGION_LENGTH characters."
        } else {
            null
        }
    }

    const val MIN_ACCOUNT_NUMBER_DIGITS = 6
    const val MAX_ACCOUNT_NUMBER_LENGTH = 32
    const val MIN_DISPLAY_NAME_LENGTH = 2
    const val MAX_DISPLAY_NAME_LENGTH = 60
    const val MAX_NOTE_LENGTH = 280
    const val MAX_REGION_LENGTH = 80
}

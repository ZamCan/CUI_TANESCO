package com.tanesco.metercompanion.core.meter

import com.tanesco.metercompanion.core.model.MeterCategory
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.PhaseConfiguration
import com.tanesco.metercompanion.core.validation.FieldError
import com.tanesco.metercompanion.core.validation.Fields
import com.tanesco.metercompanion.core.validation.ValidationResult
import com.tanesco.metercompanion.core.validation.Validators

/**
 * Raw, unvalidated input from the Meter Setup form.
 *
 * The UI collects this; it is not a domain entity until [MeterProfileValidator]
 * has approved it. Keeping the draft separate guarantees an invalid form can
 * never be half-persisted.
 */
data class MeterProfileDraft(
    val accountNumber: String = "",
    val displayName: String = "",
    val category: MeterCategory = MeterCategory.Unknown,
    val phase: PhaseConfiguration = PhaseConfiguration.Unknown,
    val region: String = "",
    val notes: String = "",
) {
    /** True when the user has not entered anything yet. */
    val isPristine: Boolean
        get() = accountNumber.isBlank() && displayName.isBlank() &&
            region.isBlank() && notes.isBlank() &&
            category == MeterCategory.Unknown && phase == PhaseConfiguration.Unknown
}

/**
 * Turns a [MeterProfileDraft] into a [MeterProfile].
 *
 * Pure and total: it either returns a valid profile or a [ValidationResult]
 * describing every problem. There is no path that throws or produces a partially
 * valid profile.
 */
class MeterProfileValidator(
    private val options: Options = Options(),
) {

    data class Options(
        /** Rejects an account number already used by another profile. */
        val requireUniqueAccountNumber: Boolean = true,
    )

    fun validate(draft: MeterProfileDraft): ValidationResult {
        val errors = buildList {
            Validators.accountNumber(draft.accountNumber)?.let {
                add(FieldError(Fields.ACCOUNT_NUMBER, it))
            }
            Validators.displayName(draft.displayName)?.let {
                add(FieldError(Fields.DISPLAY_NAME, it))
            }
            Validators.optionalRegion(draft.region)?.let {
                add(FieldError(Fields.REGION, it))
            }
            Validators.optionalNote(draft.notes)?.let {
                add(FieldError(Fields.NOTES, it))
            }
        }
        return ValidationResult.of(*errors.toTypedArray())
    }

    /**
     * Builds a profile, or returns the validation failure.
     *
     * [existingAccountNumbers] is compared in a normalised form (digits only,
     * case folded) so that `123 456` and `123-456` are treated as the same
     * number. A profile being edited passes its own current account number in
     * [excludedProfileId] so it does not collide with itself.
     */
    fun create(
        draft: MeterProfileDraft,
        id: String,
        createdAtMillis: Long,
        existingProfiles: List<MeterProfile> = emptyList(),
        excludedProfileId: String? = null,
    ): Result<MeterProfile, ValidationResult> {
        val validation = validate(draft)
        if (!validation.isValid) return Result.Invalid(validation)

        val normalized = normalizeAccountNumber(draft.accountNumber)
        val duplicate = existingProfiles.firstOrNull {
            options.requireUniqueAccountNumber &&
                it.id != excludedProfileId &&
                normalizeAccountNumber(it.accountNumber) == normalized
        }
        if (duplicate != null) {
            val conflict = ValidationResult.Valid.withField(
                Fields.ACCOUNT_NUMBER,
                "This account number is already used by \"${duplicate.displayName}\".",
            )
            return Result.Invalid(conflict)
        }

        val now = createdAtMillis
        return Result.Valid(
            MeterProfile(
                id = id,
                accountNumber = draft.accountNumber.trim(),
                displayName = draft.displayName.trim(),
                category = draft.category,
                phase = draft.phase,
                region = draft.region.trim().ifEmpty { null },
                notes = draft.notes.trim().ifEmpty { null },
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
    }

    companion object {
        /** Comparison form of an account number: digits only. */
        fun normalizeAccountNumber(raw: String): String =
            raw.filter { it.isDigit() }
    }
}

/**
 * A result that is either a valid value or a validation failure.
 *
 * Preferred over `kotlin.Result` because the failure branch carries a
 * [ValidationResult] that presentation code must handle explicitly.
 */
sealed interface Result<out T, out E> {
    data class Valid<out T>(val value: T) : Result<T, Nothing>
    data class Invalid<out E>(val error: E) : Result<Nothing, E>
}

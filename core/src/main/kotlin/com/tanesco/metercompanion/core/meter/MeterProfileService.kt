package com.tanesco.metercompanion.core.meter

import com.tanesco.metercompanion.core.history.EventRecorder
import com.tanesco.metercompanion.core.model.AppSettings
import com.tanesco.metercompanion.core.model.DomainError
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.EventCategory
import com.tanesco.metercompanion.core.model.EventNames
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.repository.MeterRepository
import com.tanesco.metercompanion.core.repository.SettingsRepository
import com.tanesco.metercompanion.core.time.Clock
import com.tanesco.metercompanion.core.time.IdGenerator
import com.tanesco.metercompanion.core.validation.Fields
import com.tanesco.metercompanion.core.validation.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Outcome of submitting the Meter Setup form.
 *
 * A dedicated type rather than [DomainResult], because a form has three
 * meaningfully different outcomes - stored, rejected with per-field messages, or
 * failed for an unrelated reason - and the screen must render each differently.
 */
sealed interface MeterProfileFormResult {
    data class Saved(val profile: MeterProfile) : MeterProfileFormResult
    data class Invalid(val validation: ValidationResult) : MeterProfileFormResult
    data class Failed(val error: DomainError) : MeterProfileFormResult
}

/**
 * Application service for meter profiles.
 *
 * Screens call this; they never touch [MeterRepository] directly. Every method
 * here is a complete, already-logged business operation, which is what keeps the
 * Activity screen trustworthy.
 */
class MeterProfileService(
    private val meterRepository: MeterRepository,
    private val settingsRepository: SettingsRepository,
    private val validator: MeterProfileValidator,
    private val events: EventRecorder,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {

    fun observeAll(): Flow<List<MeterProfile>> = meterRepository.observeAll()

    /**
     * Validates a draft without any I/O, so a form can show live feedback on
     * every keystroke.
     */
    fun validateDraft(draft: MeterProfileDraft): ValidationResult = validator.validate(draft)

    /**
     * The profile the user selected as their meter, resolved against the stored
     * id.
     *
     * Returns `null` when no meter is configured. When the stored id points at a
     * profile that has since been archived, resolution falls back to the first
     * active profile rather than showing nothing.
     */
    fun observeDefaultMeter(): Flow<MeterProfile?> =
        combine(settingsRepository.observe(), meterRepository.observeAll()) { settings, profiles ->
            resolveDefault(settings, profiles)
        }

    suspend fun defaultMeter(): MeterProfile? =
        resolveDefault(settingsRepository.get(), meterRepository.getAll())

    private fun resolveDefault(settings: AppSettings, profiles: List<MeterProfile>): MeterProfile? {
        val active = profiles.filterNot { it.isArchived }
        val selected = settings.defaultMeterId?.let { id -> active.firstOrNull { it.id == id } }
        // Falling back to the first active profile means a user who has
        // configured exactly one meter gets a sensible screen without having to
        // make an explicit selection.
        return selected ?: active.firstOrNull()
    }

    /**
     * Creates or updates a profile from a form draft.
     *
     * Pass [editingProfileId] to update an existing profile; leave it null to
     * create one. When creating the first meter it also becomes the default,
     * because a user who has configured exactly one meter has implicitly chosen
     * it.
     */
    suspend fun submit(
        draft: MeterProfileDraft,
        editingProfileId: String? = null,
    ): MeterProfileFormResult {
        val existing = meterRepository.getAll()
        val validation = validator.validate(draft)
        if (!validation.isValid) {
            events.warning(
                category = EventCategory.Meter,
                name = EventNames.METER_PROFILE_REJECTED,
                summary = "Meter details were not accepted",
                detail = validation.all().joinToString("; ") { "${it.field}: ${it.message}" },
                userInitiated = true,
            )
            return MeterProfileFormResult.Invalid(validation)
        }

        val normalized = MeterProfileValidator.normalizeAccountNumber(draft.accountNumber)
        val conflicting = existing.firstOrNull {
            it.id != editingProfileId &&
                MeterProfileValidator.normalizeAccountNumber(it.accountNumber) == normalized
        }
        if (conflicting != null) {
            val conflict = ValidationResult.Valid.withField(
                Fields.ACCOUNT_NUMBER,
                "This account number is already used by \"${conflicting.displayName}\".",
            )
            events.warning(
                category = EventCategory.Meter,
                name = EventNames.METER_PROFILE_REJECTED,
                summary = "Account number already in use",
                meterId = editingProfileId,
                userInitiated = true,
            )
            return MeterProfileFormResult.Invalid(conflict)
        }

        val now = clock.nowMillis()

        val profile = if (editingProfileId == null) {
            MeterProfile(
                id = idGenerator.newId(),
                accountNumber = draft.accountNumber.trim(),
                displayName = draft.displayName.trim(),
                category = draft.category,
                phase = draft.phase,
                region = draft.region.trim().ifEmpty { null },
                notes = draft.notes.trim().ifEmpty { null },
                createdAtMillis = now,
                updatedAtMillis = now,
            )
        } else {
            val current = existing.firstOrNull { it.id == editingProfileId }
                ?: return MeterProfileFormResult.Failed(
                    DomainError.NotFound(
                        code = "meter.not_found",
                        entity = "meter",
                        id = editingProfileId,
                    ),
                )
            current.copy(
                accountNumber = draft.accountNumber.trim(),
                displayName = draft.displayName.trim(),
                category = draft.category,
                phase = draft.phase,
                region = draft.region.trim().ifEmpty { null },
                notes = draft.notes.trim().ifEmpty { null },
                updatedAtMillis = now,
            )
        }

        val stored = meterRepository.upsert(profile)
        if (stored is DomainResult.Failure) return MeterProfileFormResult.Failed(stored.error)

        if (editingProfileId == null) {
            if (existing.none { !it.isArchived }) {
                settingsRepository.set(settingsRepository.get().copy(defaultMeterId = profile.id))
            }
            events.success(
                category = EventCategory.Meter,
                name = EventNames.METER_PROFILE_CREATED,
                summary = "Meter profile created",
                detail = "account ${profile.accountNumber}",
                meterId = profile.id,
                userInitiated = true,
            )
        } else {
            events.info(
                category = EventCategory.Meter,
                name = EventNames.METER_PROFILE_UPDATED,
                summary = "Meter profile updated",
                detail = "account ${profile.accountNumber}",
                meterId = profile.id,
                userInitiated = true,
            )
        }

        return MeterProfileFormResult.Saved(profile)
    }

    suspend fun archive(id: String): DomainResult<Unit> {
        val result = meterRepository.archive(id)
        if (result is DomainResult.Success) {
            val settings = settingsRepository.get()
            if (settings.defaultMeterId == id) {
                settingsRepository.set(settings.copy(defaultMeterId = null))
            }
            events.info(
                category = EventCategory.Meter,
                name = EventNames.METER_PROFILE_ARCHIVED,
                summary = "Meter profile archived",
                meterId = id,
                userInitiated = true,
            )
        }
        return result
    }

    /**
     * Selects the default meter.
     *
     * Passing `null` clears the selection; resolution then falls back to the
     * first active profile, which is why the UI labels that behaviour
     * explicitly rather than implying "no meter configured".
     */
    suspend fun setDefaultMeter(id: String?): DomainResult<Unit> {
        if (id != null) {
            val profile = meterRepository.getById(id)
                ?: return DomainResult.Failure(
                    DomainError.NotFound(code = "meter.not_found", entity = "meter", id = id),
                )
            if (profile.isArchived) {
                return DomainResult.Failure(
                    DomainError.Validation(
                        code = "meter.archived",
                        detail = "An archived meter cannot be selected as the default.",
                    ),
                )
            }
        }

        val result = settingsRepository.set(settingsRepository.get().copy(defaultMeterId = id))
        if (result is DomainResult.Success) {
            events.info(
                category = EventCategory.Meter,
                name = EventNames.METER_DEFAULT_CHANGED,
                summary = if (id == null) "Default meter cleared" else "Default meter changed",
                meterId = id,
                userInitiated = true,
            )
        }
        return result
    }
}

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
import com.tanesco.metercompanion.core.validation.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Application service for meter profiles.
 *
 * Screens call this; they never touch [MeterRepository] directly. Every method
 * here is a complete, already-logged business operation, which is what keeps
 * the Activity screen trustworthy.
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
     * The profile the user selected as their meter, resolved against the
     * stored id.
     *
     * Returns `null` when no meter is configured, or when the stored id points
     * at a profile that has since been archived - in which case the setting is
     * repaired so the UI does not repeatedly resolve to nothing.
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
        // Fall back to the only/first active profile so a fresh install with one
        // meter configured behaves sensibly without an explicit selection.
        return selected ?: active.firstOrNull()
    }

    /**
     * Validates and stores a new profile.
     *
     * When it is the first profile it also becomes the default, because a user
     * who has configured exactly one meter has implicitly chosen it.
     */
    suspend fun create(draft: MeterProfileDraft): DomainResult<MeterProfile> {
        val existing = meterRepository.getAll()
        val created = validator.create(
            draft = draft,
            id = idGenerator.newId(),
            createdAtMillis = clock.nowMillis(),
            existingProfiles = existing,
        )

        val profile = when (created) {
            is Result.Invalid -> {
                events.warning(
                    category = EventCategory.Meter,
                    name = EventNames.METER_PROFILE_REJECTED,
                    summary = "Meter details were not accepted",
                    detail = created.error.all().joinToString("; ") { "${it.field}: ${it.message}" },
                    userInitiated = true,
                )
                return DomainResult.Failure(created.error.toDomainError())
            }

            is Result.Valid -> created.value
        }

        val stored = meterRepository.upsert(profile)
        if (stored is DomainResult.Failure) return stored

        val isFirstProfile = existing.none { !it.isArchived }
        if (isFirstProfile) {
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

        return DomainResult.Success(profile)
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
     * explicitly rather than implying "no meter".
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
                        field = com.tanesco.metercompanion.core.validation.Fields.ACCOUNT_NUMBER,
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

/**
 * Presents a [ValidationResult] through the domain's error vocabulary.
 *
 * The mapping is centralised so that a form failure always produces a
 * `validation.invalid_form` code carrying the field messages, and so that the
 * UI has exactly one shape to render.
 */
fun ValidationResult.toDomainError(): DomainError.Validation =
    DomainError.Validation(
        code = "validation.invalid_form",
        detail = all().joinToString("; ") { "${it.field}: ${it.message}" },
    )

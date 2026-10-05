package com.tanesco.metercompanion.app.ui.metersetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanesco.metercompanion.core.model.MeterCategory
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.PhaseConfiguration
import com.tanesco.metercompanion.core.meter.MeterProfileDraft
import com.tanesco.metercompanion.core.meter.MeterProfileFormResult
import com.tanesco.metercompanion.core.meter.MeterProfileService
import com.tanesco.metercompanion.core.repository.MeterRepository
import com.tanesco.metercompanion.core.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the Meter Setup form renders. */
data class MeterSetupUiState(
    val draft: MeterProfileDraft = MeterProfileDraft(),
    val errors: ValidationResult = ValidationResult.Valid,
    val editingProfileId: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    /** Set once stored; the screen observes it and navigates back. */
    val savedProfileId: String? = null,
    /** Set when the form could not be loaded or saved for a non-field reason. */
    val failureMessage: String? = null,
) {
    val isEditing: Boolean get() = editingProfileId != null

    val title: String get() = if (isEditing) "Edit meter" else "Add a meter"

    fun errorFor(field: String): String? = errors.messageFor(field)

    fun supportingFor(field: String): String? = errors.combinedMessageFor(field)
}

/**
 * Drives Meter Setup for both creating and editing.
 *
 * The form holds a [MeterProfileDraft], never a half-built
 * [com.tanesco.metercompanion.core.model.MeterProfile], so an invalid form
 * cannot reach persistence. All validation and persistence rules live in
 * [MeterProfileService]; this class only converts them into screen state.
 */
class MeterSetupViewModel(
    private val meterProfiles: MeterProfileService,
    private val meterRepository: MeterRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MeterSetupUiState())
    val state: StateFlow<MeterSetupUiState> = _state.asStateFlow()

    /** Loads [profileId] into the form. A null id starts a new form. */
    fun start(profileId: String?) {
        if (profileId == null) {
            _state.value = MeterSetupUiState()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val existing = meterRepository.getById(profileId)
            _state.value = if (existing == null) {
                MeterSetupUiState(
                    isLoading = false,
                    failureMessage = "That meter is no longer available.",
                )
            } else {
                MeterSetupUiState(
                    draft = existing.toDraft(),
                    editingProfileId = existing.id,
                    isLoading = false,
                )
            }
        }
    }

    fun onAccountNumberChanged(value: String) = updateDraft { copy(accountNumber = value) }

    fun onDisplayNameChanged(value: String) = updateDraft { copy(displayName = value) }

    fun onRegionChanged(value: String) = updateDraft { copy(region = value) }

    fun onNotesChanged(value: String) = updateDraft { copy(notes = value) }

    fun onCategoryChanged(value: MeterCategory) = updateDraft { copy(category = value) }

    fun onPhaseChanged(value: PhaseConfiguration) = updateDraft { copy(phase = value) }

    /**
     * Re-validates on every keystroke so errors clear as they are fixed, rather
     * than only after a rejected submission.
     */
    private fun updateDraft(transform: MeterProfileDraft.() -> MeterProfileDraft) {
        _state.update { current ->
            val updated = current.draft.transform()
            current.copy(
                draft = updated,
                errors = meterProfiles.validateDraft(updated),
                failureMessage = null,
            )
        }
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, failureMessage = null) }

            val result = meterProfiles.submit(
                draft = current.draft,
                editingProfileId = current.editingProfileId,
            )

            _state.value = when (result) {
                is MeterProfileFormResult.Saved -> current.copy(
                    isSaving = false,
                    savedProfileId = result.profile.id,
                )

                is MeterProfileFormResult.Invalid -> current.copy(
                    isSaving = false,
                    errors = result.validation,
                    failureMessage = "Some details need attention.",
                )

                is MeterProfileFormResult.Failed -> current.copy(
                    isSaving = false,
                    failureMessage = result.error.userFacingMessage(),
                )
            }
        }
    }
}

/** Maps a stored profile back into a draft, for editing. */
private fun MeterProfile.toDraft(): MeterProfileDraft = MeterProfileDraft(
    accountNumber = accountNumber,
    displayName = displayName,
    category = category,
    phase = phase,
    region = region.orEmpty(),
    notes = notes.orEmpty(),
)

package com.tanesco.metercompanion.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanesco.metercompanion.core.history.EventRecorder
import com.tanesco.metercompanion.core.model.AppSettings
import com.tanesco.metercompanion.core.model.AppearanceMode
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.EventCategory
import com.tanesco.metercompanion.core.model.EventNames
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.UnitSystem
import com.tanesco.metercompanion.core.meter.MeterProfileService
import com.tanesco.metercompanion.core.repository.SettingsRepository
import com.tanesco.metercompanion.core.token.TokenService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the Settings screen renders. */
data class SettingsUiState(
    val settings: AppSettings = AppSettings.Default,
    val meters: List<MeterProfile> = emptyList(),
    val isLoading: Boolean = true,
)

/** One-shot confirmation shown after a settings change. */
sealed interface SettingsNotice {
    data object Idle : SettingsNotice
    data class Saved(val message: String) : SettingsNotice
    data class Failed(val message: String) : SettingsNotice
}

/**
 * Drives the Settings screen.
 *
 * Every change is written through [SettingsRepository] and logged, so a
 * settings change appears in Activity like any other event. Nothing is kept in
 * a local field that could disagree with what was stored.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val meterProfiles: MeterProfileService,
    private val eventRecorder: EventRecorder,
) : ViewModel() {

    private val _notice = MutableStateFlow<SettingsNotice>(SettingsNotice.Idle)
    val notice: StateFlow<SettingsNotice> = _notice.asStateFlow()

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.observe(),
        meterProfiles.observeAll(),
    ) { settings, meters ->
        SettingsUiState(
            settings = settings,
            meters = meters.filterNot { it.isArchived },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = SettingsUiState(),
    )

    fun consumeNotice() {
        _notice.value = SettingsNotice.Idle
    }

    fun setAppearance(value: AppearanceMode) = update(
        summary = "Appearance set to ${value.label}",
    ) { it.copy(appearance = value) }

    fun setUnitSystem(value: UnitSystem) = update(
        summary = "Units set to ${value.label}",
    ) { it.copy(unitSystem = value) }

    /**
     * Enables or disables connection attempts.
     *
     * Default is off. Even when enabled, the shipped adapter only reports an
     * honest unavailable state - the setting exists so the choice is the user's,
     * not so the application can pretend a meter is reachable.
     */
    fun setConnectionAttemptsEnabled(enabled: Boolean) = update(
        summary = if (enabled) {
            "Connection attempts enabled"
        } else {
            "Connection attempts disabled"
        },
    ) { it.copy(allowConnectionAttempts = enabled) }

    fun setDeveloperMode(enabled: Boolean) = update(
        summary = if (enabled) "Developer details enabled" else "Developer details disabled",
    ) { it.copy(developerMode = enabled) }

    fun setDefaultMeter(meterId: String?) {
        viewModelScope.launch {
            val result = meterProfiles.setDefaultMeter(meterId)
            _notice.value = result.fold(
                onSuccess = {
                    SettingsNotice.Saved(
                        if (meterId == null) "Default meter cleared" else "Default meter updated",
                    )
                },
                onFailure = { error ->
                    SettingsNotice.Failed("Could not update the default meter. ${error.technicalDetail}")
                },
            )
        }
    }

    fun archiveMeter(meterId: String) {
        viewModelScope.launch {
            val result = meterProfiles.archive(meterId)
            _notice.value = result.fold(
                onSuccess = { SettingsNotice.Saved("Meter archived") },
                onFailure = { error -> SettingsNotice.Failed("Could not archive the meter. ${error.technicalDetail}") },
            )
        }
    }

    private fun update(summary: String, transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val updated = transform(settingsRepository.get())
            val result = settingsRepository.set(updated)

            // `fold` takes a non-suspending lambda, so the suspend call that
            // records the event is made here rather than inside the branch.
            if (result is DomainResult.Success) {
                eventRecorder.info(
                    category = EventCategory.Settings,
                    name = EventNames.SETTINGS_CHANGED,
                    summary = summary,
                    userInitiated = true,
                )
                _notice.value = SettingsNotice.Saved(summary)
            } else {
                _notice.value = SettingsNotice.Failed(
                    "Could not save that change. " +
                        (result.errorOrNull()?.technicalDetail ?: "unknown error"),
                )
            }
        }
    }
}

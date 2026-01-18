package com.minifocus.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minifocus.launcher.manager.SettingsManager
import com.minifocus.launcher.permissions.PermissionsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 11, // Welcome, Why, Launcher, Perms, 6 features, Finish
    val launcherRoleGranted: Boolean = false,
    val permissionsState: PermissionsState = PermissionsState(
        notificationsGranted = false,
        notificationListenerGranted = false,
        lockAccessibilityGranted = false,
        exactAlarmsGranted = false,
        usageStatsGranted = false,
        overlayGranted = false
    ),
    
    // Personalization choices (temporary until commit)
    val showSeconds: Boolean? = null,
    val smartSuggestionsEnabled: Boolean? = null,
    val keyboardOnSwipe: Boolean? = null,
    val showDailyTasksOnHome: Boolean? = null,
    val notificationInboxEnabled: Boolean? = null,
    val doubleTapLockScreen: Boolean? = null,
    
    val isCompleting: Boolean = false
)

class OnboardingViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _completionEvent = MutableStateFlow(false)
    val completionEvent: StateFlow<Boolean> = _completionEvent.asStateFlow()

    fun setCurrentStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step)
        viewModelScope.launch {
            settingsManager.setOnboardingStep(step)
        }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < _uiState.value.totalSteps - 1) {
            setCurrentStep(current + 1)
        }
    }

    fun previousStep() {
        val current = _uiState.value.currentStep
        if (current > 0) {
            setCurrentStep(current - 1)
        }
    }

    fun setLauncherRoleGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(launcherRoleGranted = granted)
    }

    fun updatePermissionsState(state: PermissionsState) {
        _uiState.value = _uiState.value.copy(permissionsState = state)
    }

    fun setShowSeconds(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showSeconds = enabled)
    }

    fun setSmartSuggestions(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(smartSuggestionsEnabled = enabled)
    }

    fun setKeyboardOnSwipe(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(keyboardOnSwipe = enabled)
    }

    fun setShowDailyTasksOnHome(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showDailyTasksOnHome = enabled)
    }

    fun setNotificationInbox(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationInboxEnabled = enabled)
    }

    fun setDoubleTapLock(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(doubleTapLockScreen = enabled)
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true)
            
            // Commit all preferences
            _uiState.value.showSeconds?.let { settingsManager.setShowSeconds(it) }
            _uiState.value.smartSuggestionsEnabled?.let { settingsManager.setSmartSuggestionsEnabled(it) }
            _uiState.value.keyboardOnSwipe?.let { settingsManager.setKeyboardSearchOnSwipe(it) }
            _uiState.value.showDailyTasksOnHome?.let { settingsManager.setShowDailyTasksOnHome(it) }
            _uiState.value.notificationInboxEnabled?.let { settingsManager.setNotificationInboxEnabled(it) }
            _uiState.value.doubleTapLockScreen?.let { settingsManager.setDoubleTapLockScreen(it) }
            
            // Mark onboarding complete
            settingsManager.setSetupCompleted(true)
            settingsManager.setPermissionOnboardingAcknowledged(true)
            settingsManager.setOnboardingStep(0)
            
            // Emit completion event
            _completionEvent.value = true
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            settingsManager.setSetupCompleted(false)
            settingsManager.setOnboardingStep(0)
            setCurrentStep(0)
        }
    }
}

class OnboardingViewModelFactory(
    private val settingsManager: SettingsManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            return OnboardingViewModel(settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

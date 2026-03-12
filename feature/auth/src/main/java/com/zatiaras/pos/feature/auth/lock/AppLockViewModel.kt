package com.zatiaras.pos.feature.auth.lock

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.feature.auth.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AppLockUiState(
    val isUnlocked: Boolean = false,
    val enteredPin: String = "",
    val pinError: Boolean = false,
    val errorMessage: String? = null,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val pinSet: Boolean = false,
    val showPinInput: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockPreferences: AppLockPreferences,
    private val biometricManager: AppBiometricManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    init {
        loadLockSettings()
    }

    private fun loadLockSettings() {
        viewModelScope.launch {
            val biometricEnabled = appLockPreferences.isBiometricEnabledNow()
            val biometricAvailable = biometricManager.isBiometricAvailable()
            val pinSet = appLockPreferences.isPinSetNow()
            
            _uiState.update { state ->
                state.copy(
                    biometricEnabled = biometricEnabled,
                    biometricAvailable = biometricAvailable,
                    pinSet = pinSet,
                    // Show PIN input if biometric is not available but PIN is set
                    showPinInput = pinSet && (!biometricEnabled || !biometricAvailable)
                )
            }

            Timber.d("Lock settings loaded: biometric=$biometricEnabled, available=$biometricAvailable, pinSet=$pinSet")
        }
    }

    fun onPinDigitClick(digit: String) {
        val currentPin = _uiState.value.enteredPin
        if (currentPin.length >= 4) return

        val newPin = currentPin + digit
        _uiState.update { state ->
            state.copy(
                enteredPin = newPin,
                pinError = false,
                errorMessage = null
            )
        }

        // Auto-verify when 4 digits entered
        if (newPin.length == 4) {
            verifyPin(newPin)
        }
    }

    fun onBackspaceClick() {
        val currentPin = _uiState.value.enteredPin
        if (currentPin.isEmpty()) return

        _uiState.update { state ->
            state.copy(
                enteredPin = currentPin.dropLast(1),
                pinError = false,
                errorMessage = null
            )
        }
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            val lockoutRemaining = appLockPreferences.getPinLockoutRemainingMillis()
            if (lockoutRemaining > 0L) {
                _uiState.update { state ->
                    state.copy(
                        enteredPin = "",
                        pinError = true,
                        errorMessage = formatLockoutMessage(lockoutRemaining),
                        isLoading = false
                    )
                }
                return@launch
            }

            // Add a small delay to allow the user to see the 4th dot filled
            // before showing the verification result
            delay(200)
            
            _uiState.update { it.copy(isLoading = true) }
            
            val isValid = appLockPreferences.verifyPin(pin)
            
            if (isValid) {
                Timber.d("PIN verified successfully")
                appLockPreferences.clearPinLockout()
                _uiState.update { state ->
                    state.copy(
                        isUnlocked = true,
                        isLoading = false
                    )
                }
            } else {
                Timber.w("Invalid PIN entered")
                val lockoutDuration = appLockPreferences.recordFailedPinAttempt()
                val errorMessage = if (lockoutDuration > 0L) {
                    formatLockoutMessage(lockoutDuration)
                } else {
                    context.getString(R.string.app_lock_pin_wrong)
                }
                _uiState.update { state ->
                    state.copy(
                        enteredPin = "",
                        pinError = true,
                        errorMessage = errorMessage,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun formatLockoutMessage(remainingMillis: Long): String {
        val seconds = (remainingMillis / 1000).coerceAtLeast(1)
        return if (seconds >= 60) {
            val minutes = (seconds + 59) / 60
            "Terlalu banyak percobaan. Coba lagi dalam $minutes menit."
        } else {
            "Terlalu banyak percobaan. Coba lagi dalam $seconds detik."
        }
    }

    fun authenticateWithBiometric(activity: FragmentActivity) {
        if (!_uiState.value.biometricAvailable) {
            // Fall back to PIN input
            _uiState.update { it.copy(showPinInput = true) }
            return
        }

        biometricManager.authenticate(
            activity = activity,
            title = context.getString(R.string.app_lock_title),
            subtitle = context.getString(R.string.app_lock_bio_hint),
            negativeButtonText = context.getString(R.string.bio_prompt_neg_button)
        ) { result ->
            when (result) {
                is BiometricResult.Success -> {
                    Timber.d("Biometric authentication successful")
                    _uiState.update { it.copy(isUnlocked = true) }
                }
                is BiometricResult.Cancelled -> {
                    Timber.d("Biometric cancelled, showing PIN input")
                    _uiState.update { it.copy(showPinInput = true) }
                }
                is BiometricResult.Error -> {
                    Timber.w("Biometric error: ${result.message}")
                    _uiState.update { state ->
                        state.copy(
                            showPinInput = true,
                            errorMessage = result.message
                        )
                    }
                }
                is BiometricResult.NotAvailable,
                is BiometricResult.NotEnrolled -> {
                    _uiState.update { it.copy(showPinInput = true) }
                }
            }
        }
    }
}

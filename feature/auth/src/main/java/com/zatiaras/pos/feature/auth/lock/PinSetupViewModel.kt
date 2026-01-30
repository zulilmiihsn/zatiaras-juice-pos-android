package com.zatiaras.pos.feature.auth.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PinSetupUiState(
    val step: PinSetupStep = PinSetupStep.ENTER_NEW_PIN,
    val currentPin: String = "",
    val newPin: String = "",
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val isPinSet: Boolean = false,
    val isChangingPin: Boolean = false
)

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    private val appLockPreferences: AppLockPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinSetupUiState())
    val uiState: StateFlow<PinSetupUiState> = _uiState.asStateFlow()

    init {
        checkIfPinExists()
    }

    private fun checkIfPinExists() {
        viewModelScope.launch {
            val pinExists = appLockPreferences.isPinSetNow()
            if (pinExists) {
                // If PIN exists, we're changing it, so verify current first
                _uiState.update { it.copy(isChangingPin = true) }
            }
        }
    }

    fun setIsChangingPin(isChanging: Boolean) {
        viewModelScope.launch {
            val pinExists = appLockPreferences.isPinSetNow()
            if (isChanging && pinExists) {
                _uiState.update { 
                    it.copy(
                        isChangingPin = true,
                        step = PinSetupStep.VERIFY_CURRENT_PIN
                    ) 
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isChangingPin = false,
                        step = PinSetupStep.ENTER_NEW_PIN
                    ) 
                }
            }
        }
    }

    fun onDigitClick(digit: String) {
        val currentPinInput = _uiState.value.currentPin
        if (currentPinInput.length >= 4) return

        val newPinInput = currentPinInput + digit
        _uiState.update { state ->
            state.copy(
                currentPin = newPinInput,
                hasError = false,
                errorMessage = null
            )
        }

        // Auto-process when 4 digits entered
        if (newPinInput.length == 4) {
            processPin(newPinInput)
        }
    }

    fun onBackspaceClick() {
        val currentPinInput = _uiState.value.currentPin
        if (currentPinInput.isEmpty()) return

        _uiState.update { state ->
            state.copy(
                currentPin = currentPinInput.dropLast(1),
                hasError = false,
                errorMessage = null
            )
        }
    }

    private fun processPin(pin: String) {
        viewModelScope.launch {
            when (_uiState.value.step) {
                PinSetupStep.VERIFY_CURRENT_PIN -> {
                    verifyCurrentPin(pin)
                }
                PinSetupStep.ENTER_NEW_PIN -> {
                    // Add small delay so user can see 4th dot filled before transition
                    delay(200)
                    // Store the new PIN and move to confirmation
                    _uiState.update { state ->
                        state.copy(
                            newPin = pin,
                            currentPin = "",
                            step = PinSetupStep.CONFIRM_PIN
                        )
                    }
                }
                PinSetupStep.CONFIRM_PIN -> {
                    confirmPin(pin)
                }
            }
        }
    }

    private suspend fun verifyCurrentPin(pin: String) {
        val isValid = appLockPreferences.verifyPin(pin)
        
        if (isValid) {
            Timber.d("Current PIN verified")
            _uiState.update { state ->
                state.copy(
                    currentPin = "",
                    step = PinSetupStep.ENTER_NEW_PIN
                )
            }
        } else {
            Timber.w("Current PIN verification failed")
            _uiState.update { state ->
                state.copy(
                    currentPin = "",
                    hasError = true,
                    errorMessage = "PIN salah"
                )
            }
        }
    }

    private suspend fun confirmPin(pin: String) {
        val newPin = _uiState.value.newPin
        
        if (pin == newPin) {
            // PINs match, save it
            try {
                appLockPreferences.setPin(pin)
                // Also enable lock when PIN is set
                appLockPreferences.setLockEnabled(true)
                
                Timber.d("PIN set successfully")
                _uiState.update { state ->
                    state.copy(
                        isPinSet = true,
                        hasError = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save PIN")
                _uiState.update { state ->
                    state.copy(
                        currentPin = "",
                        hasError = true,
                        errorMessage = "Gagal menyimpan PIN"
                    )
                }
            }
        } else {
            // PINs don't match
            Timber.w("PIN confirmation failed - mismatch")
            _uiState.update { state ->
                state.copy(
                    currentPin = "",
                    hasError = true,
                    errorMessage = "PIN tidak cocok"
                )
            }
            
            // Reset to enter new PIN after delay
            delay(1500)
            _uiState.update { state ->
                state.copy(
                    newPin = "",
                    step = PinSetupStep.ENTER_NEW_PIN,
                    hasError = false,
                    errorMessage = null
                )
            }
        }
    }
}

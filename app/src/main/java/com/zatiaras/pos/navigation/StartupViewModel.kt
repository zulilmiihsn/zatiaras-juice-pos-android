package com.zatiaras.pos.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.repository.LocalAuthRepository
import com.zatiaras.pos.core.data.session.SessionPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface StartupState {
    data object Loading : StartupState
    data object NeedsLogin : StartupState
    data object SessionRestored : StartupState
    data object SessionExpired : StartupState
}

/**
 * ViewModel for handling app startup and session restoration.
 * 
 * Checks for:
 * 1. Saved session existence
 * 2. Session expiry (8 hours default)
 * 3. User still active in database
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val localAuthRepository: LocalAuthRepository,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            Timber.d("Checking for saved session...")
            
            // Check if session exists
            if (!sessionPreferences.isLoggedIn()) {
                Timber.d("No saved session, need to login")
                _state.value = StartupState.NeedsLogin
                syncUsersInBackground()
                return@launch
            }
            
            // Check if session expired
            if (sessionPreferences.isSessionExpired()) {
                Timber.d("Session expired, need to re-login")
                sessionPreferences.clearSession()
                _state.value = StartupState.SessionExpired
                syncUsersInBackground()
                return@launch
            }
            
            // Try to restore session from preferences
            val sessionRestored = localAuthRepository.restoreSession()
            
            if (sessionRestored) {
                Timber.d("Session restored successfully")
                // Refresh session timestamp to extend timeout
                sessionPreferences.refreshSession()
                _state.value = StartupState.SessionRestored
            } else {
                Timber.d("Failed to restore session (user not found or inactive)")
                sessionPreferences.clearSession()
                _state.value = StartupState.NeedsLogin
            }
            
            syncUsersInBackground()
        }
    }
    
    private fun syncUsersInBackground() {
        viewModelScope.launch {
            localAuthRepository.syncUsersFromRemote()
        }
    }
}

package com.zatiaras.pos.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.repository.LocalAuthRepository
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
}

/**
 * ViewModel for handling app startup and session restoration.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val localAuthRepository: LocalAuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            Timber.d("Checking for saved session...")
            
            // First try to restore session
            val sessionRestored = localAuthRepository.restoreSession()
            
            if (sessionRestored) {
                Timber.d("Session restored successfully")
                _state.value = StartupState.SessionRestored
            } else {
                Timber.d("No valid session, need to login")
                _state.value = StartupState.NeedsLogin
            }
            
            // Also sync users in background (don't block)
            viewModelScope.launch {
                localAuthRepository.syncUsersFromRemote()
            }
        }
    }
}

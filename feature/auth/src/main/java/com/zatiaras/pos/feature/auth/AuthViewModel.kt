package com.zatiaras.pos.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.repository.LocalAuthRepository
import com.zatiaras.pos.core.domain.Result
import com.zatiaras.pos.core.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import com.zatiaras.pos.core.ui.util.UiText
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Syncing : AuthUiState // Syncing users from Supabase
    data object Success : AuthUiState
    data class Error(val message: UiText) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val localAuthRepository: LocalAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<UiText?>(null)
    val syncStatus: StateFlow<UiText?> = _syncStatus.asStateFlow()

    init {
        syncUsersOnStartup()
    }

    /**
     * Sync users from Supabase on app startup.
     * If sync fails (offline), we can still login with cached local users.
     */
    private fun syncUsersOnStartup() {
        viewModelScope.launch {
            _uiState.update { AuthUiState.Syncing }
            _syncStatus.value = UiText.StringResource(R.string.auth_syncing_data)
            
            when (val result = localAuthRepository.syncUsersWithResult()) {
                is Result.Success -> {
                    val syncedCount = result.data
                    Timber.d("User sync successful: $syncedCount users")
                    _syncStatus.value = UiText.StringResource(R.string.auth_sync_success_count, syncedCount)
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Sync failed: ${result.exception?.message}")
                    
                    // Check if we have local users for offline mode
                    val localUsers = localAuthRepository.getAllUsers()
                    if (localUsers.isEmpty()) {
                        _syncStatus.value = UiText.StringResource(R.string.auth_no_connection)
                    } else {
                        _syncStatus.value = UiText.StringResource(R.string.auth_offline_mode, localUsers.size)
                    }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
            
            _uiState.update { AuthUiState.Idle }
        }
    }
    
    /**
     * Manual sync triggered by user.
     */
    fun resync() {
        syncUsersOnStartup()
    }

    fun login(username: String, password: String, branchId: String) {
        viewModelScope.launch {
            if (branchId.isEmpty()) {
                _uiState.update { AuthUiState.Error(UiText.StringResource(R.string.auth_error_branch_required)) }
                return@launch
            }
            
            _uiState.update { AuthUiState.Loading }
            
            // TODO(P2): Validasi role-user terhadap branch membutuhkan dukungan endpoint backend.
            // For now, we assume the user exists and credential is correct
            
            when (val result = loginUseCase(username, password)) {
                is Result.Success -> {
                    // Logic to store selected branch pref can go here
                    // e.g. sessionPreferences.saveBranchId(branchId)
                    _uiState.update { AuthUiState.Success }
                }
                is Result.Error -> {
                    val errorMessage = result.exception?.message ?: ""
                    val uiError = if (errorMessage.isNotBlank()) {
                        UiText.DynamicString(errorMessage)
                    } else {
                        UiText.StringResource(R.string.auth_error_login_failed)
                    }
                    _uiState.update { AuthUiState.Error(uiError) }
                }
                else -> Unit
            }
        }
    }
    
    fun resetState() {
        _uiState.update { AuthUiState.Idle }
    }
}

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
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Syncing : AuthUiState // Syncing users from Supabase
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val localAuthRepository: LocalAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

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
            _syncStatus.value = "Menyinkronkan data..."
            
            try {
                val syncedCount = localAuthRepository.syncUsersFromRemote()
                
                if (syncedCount >= 0) {
                    Timber.d("User sync successful: $syncedCount users")
                    _syncStatus.value = "Tersinkronkan: $syncedCount user"
                } else {
                    // Sync failed (offline) - check if we have local users
                    val localUsers = localAuthRepository.getAllUsers()
                    if (localUsers.isEmpty()) {
                        _syncStatus.value = "Tidak ada koneksi. Belum ada data user."
                        Timber.w("No network and no local users available")
                    } else {
                        _syncStatus.value = "Mode offline (${localUsers.size} user tersedia)"
                        Timber.d("Offline mode with ${localUsers.size} local users")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Sync error: ${e.message}")
                _syncStatus.value = "Mode offline"
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

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { AuthUiState.Loading }
            when (val result = loginUseCase(username, password)) {
                is Result.Success -> {
                    _uiState.update { AuthUiState.Success }
                }
                is Result.Error -> {
                    _uiState.update { AuthUiState.Error(result.exception?.message ?: "Login gagal") }
                }
                else -> Unit
            }
        }
    }
    
    fun resetState() {
        _uiState.update { AuthUiState.Idle }
    }
}

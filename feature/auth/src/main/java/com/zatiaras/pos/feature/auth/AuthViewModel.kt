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
    data object Success : AuthUiState
    data object FirstRun : AuthUiState // New state for first-time setup
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val localAuthRepository: LocalAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkFirstRun()
    }

    private fun checkFirstRun() {
        viewModelScope.launch {
            if (localAuthRepository.isFirstRun()) {
                Timber.d("First run detected, setting up default admin user")
                // Auto-create default admin for first run
                localAuthRepository.setupDefaultAdmin(
                    username = "admin",
                    password = "admin123",
                    displayName = "Administrator"
                )
                Timber.d("Default admin created: admin / admin123")
            }
        }
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

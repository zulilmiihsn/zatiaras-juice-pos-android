package com.zatiaras.pos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val branchName: String = "Zatiaras Juice",
    val isLoading: Boolean = false,
    val isLoggedOut: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            // TODO: Fetch user details from Supabase when we have user profile table
            // For now, we'll use placeholder data
            _uiState.update { 
                it.copy(
                    userName = "Owner",
                    branchName = "Zatiaras Juice"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                authRepository.logout()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isLoggedOut = true
                    )
                }
                Timber.d("User logged out successfully")
            } catch (e: Exception) {
                Timber.e(e, "Logout failed")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Logout failed"
                    )
                }
            }
        }
    }
}

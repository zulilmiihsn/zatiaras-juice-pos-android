package com.zatiaras.pos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.domain.AuthRepository
import com.zatiaras.pos.core.domain.model.StoreSession
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

data class DashboardMetrics(
    val revenue: Long = 0,
    val transactions: Int = 0,
    val itemsSold: Int = 0,
    val profit: Long = 0 // Placeholder for now
)

data class HomeUiState(
    val userName: String = "",
    val branchName: String = "Zatiaras Juice",
    val isLoading: Boolean = false,
    val isLoggedOut: Boolean = false,
    val errorMessage: String? = null,
    val session: StoreSession? = null,
    val metrics: DashboardMetrics = DashboardMetrics()
) {
    val isStoreOpen: Boolean get() = session?.isActive == true
}

sealed class HomeEvent {
    object Logout : HomeEvent()
    data class OpenStore(val initialCash: Long) : HomeEvent()
    object CloseStore : HomeEvent()
    object RefreshStats : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storeSessionRepository: StoreSessionRepository,
    private val transactionDao: TransactionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
        observeSession()
        loadDashboardStats()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            // TODO: Fetch user details from Supabase when we have user profile table
            _uiState.update { 
                it.copy(
                    userName = "Owner",
                    branchName = "Zatiaras Juice"
                )
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            storeSessionRepository.getActiveSession().collectLatest { session ->
                _uiState.update { it.copy(session = session) }
            }
        }
    }

    private fun loadDashboardStats() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val calendar = Calendar.getInstance()
                val endOfDay = calendar.timeInMillis
                
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                val revenue = transactionDao.getTotalRevenueForDay(startOfDay, endOfDay)
                val itemsSold = transactionDao.getTotalItemsSoldForDay(startOfDay, endOfDay)
                val transactions = transactionDao.getTransactionCountForDay(startOfDay, endOfDay)
                
                // Profit calculation requires COGS, simplified for now
                val profit = (revenue * 0.4).toLong() // Mock 40% margin

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        metrics = DashboardMetrics(
                            revenue = revenue,
                            itemsSold = itemsSold,
                            transactions = transactions,
                            profit = profit
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load dashboard stats")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Logout -> logout()
            is HomeEvent.OpenStore -> openStore(event.initialCash)
            is HomeEvent.CloseStore -> closeStore()
            is HomeEvent.RefreshStats -> loadDashboardStats()
        }
    }

    private fun openStore(initialCash: Long) {
        viewModelScope.launch {
            try {
                storeSessionRepository.openSession(initialCash)
                // Session update will be caught by observer
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Gagal membuka toko: ${e.message}") }
            }
        }
    }

    private fun closeStore() {
        viewModelScope.launch {
            try {
                val currentSession = _uiState.value.session
                if (currentSession != null) {
                    storeSessionRepository.closeSession(currentSession.id)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Gagal menutup toko: ${e.message}") }
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
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

package com.zatiaras.pos.feature.reports.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ReportDashboardViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportDashboardUiState())
    val uiState: StateFlow<ReportDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Load all data in parallel
                val stats = reportRepository.getDashboardStats()
                val weeklyRevenue = reportRepository.getDailyRevenueHistory(7)
                
                // Get top products for this month
                val calendar = Calendar.getInstance()
                val endDate = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val startDate = calendar.timeInMillis
                
                val topProducts = reportRepository.getTopSellingProducts(
                    startDate = startDate,
                    endDate = endDate,
                    limit = 5
                )
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        stats = stats,
                        weeklyRevenue = weeklyRevenue,
                        topProducts = topProducts
                    )
                }
                
                Timber.d("Dashboard loaded: ${stats.todayTransactions} transactions today")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load dashboard data")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Gagal memuat data"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }
}

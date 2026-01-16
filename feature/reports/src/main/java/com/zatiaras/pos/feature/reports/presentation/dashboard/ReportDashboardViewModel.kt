package com.zatiaras.pos.feature.reports.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
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
    private val reportRepository: ReportRepository,
    private val storeSessionRepository: StoreSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportDashboardUiState())
    val uiState: StateFlow<ReportDashboardUiState> = _uiState.asStateFlow()

    init {
        observeStoreSession()
        loadDashboardData()
    }

    private fun observeStoreSession() {
        viewModelScope.launch {
            storeSessionRepository.getActiveSession().collect { session ->
                _uiState.update { it.copy(isStoreOpen = session != null) }
            }
        }
    }

    fun openStore(openingBalance: Long) {
        viewModelScope.launch {
            storeSessionRepository.openSession(openingBalance)
        }
    }

    fun closeStore() {
        viewModelScope.launch {
            val session = storeSessionRepository.getActiveSessionOneShot()
            session?.let {
                storeSessionRepository.closeSession(it.id)
            }
        }
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
                
                // Calculate statistics - only if there's actual data
                val totalTransactions = weeklyRevenue.sumOf { it.transactionCount }
                val totalRevenue = weeklyRevenue.sumOf { it.revenue }
                
                // 1. Average transactions per day
                val avgTransactions = if (totalTransactions > 0 && weeklyRevenue.isNotEmpty()) {
                    totalTransactions / weeklyRevenue.size
                } else {
                    0
                }
                
                // 2. Peak hours - placeholder until we have timestamp data
                val peakHours = if (totalTransactions > 0) {
                    "16.00-17.00"
                } else {
                    "-"
                }
                
                // 3. Average Order Value (AOV)
                val avgOrderValue = if (stats.todayTransactions > 0) {
                    stats.todayRevenue / stats.todayTransactions
                } else if (totalTransactions > 0) {
                    totalRevenue / totalTransactions
                } else {
                    0L
                }
                
                // 4. Average items per transaction
                val avgItems = if (stats.todayTransactions > 0) {
                    stats.todayItemsSold.toDouble() / stats.todayTransactions
                } else {
                    0.0
                }
                
                // 5. Growth percent vs yesterday (using stats.revenueGrowthPercent)
                val growth = if (totalRevenue > 0) {
                    stats.revenueGrowthPercent
                } else {
                    null
                }
                
                // 6. Busiest day of the week
                val busiestDay = if (weeklyRevenue.isNotEmpty()) {
                    val maxDay = weeklyRevenue.maxByOrNull { it.transactionCount }
                    if (maxDay != null && maxDay.transactionCount > 0) {
                        java.text.SimpleDateFormat("EEEE", java.util.Locale("id", "ID"))
                            .format(java.util.Date(maxDay.date))
                    } else {
                        "-"
                    }
                } else {
                    "-"
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        stats = stats,
                        weeklyRevenue = weeklyRevenue,
                        topProducts = topProducts,
                        averageTransactionsPerDay = avgTransactions,
                        peakHours = peakHours,
                        averageOrderValue = avgOrderValue,
                        averageItemsPerTransaction = avgItems,
                        growthPercent = growth,
                        busiestDay = busiestDay
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



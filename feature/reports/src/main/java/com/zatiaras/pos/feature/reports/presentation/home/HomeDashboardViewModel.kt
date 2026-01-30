package com.zatiaras.pos.feature.reports.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.access.AccessControlManager
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
import com.zatiaras.pos.core.domain.util.DateUtils
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Home Dashboard Screen.
 * Manages dashboard data including today's stats, top products, and store session.
 */
@HiltViewModel
class HomeDashboardViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val storeSessionRepository: StoreSessionRepository,
    private val accessControlManager: AccessControlManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeDashboardUiState())
    val uiState: StateFlow<HomeDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        observeStoreSession()
        observeUserRole()
    }

    /**
     * Observe user role (Owner or not)
     */
    private fun observeUserRole() {
        viewModelScope.launch {
            accessControlManager.isOwner().collect { isOwner ->
                _uiState.update { it.copy(isOwner = isOwner) }
            }
        }
    }

    /**
     * Verify PIN for protected actions.
     */
    suspend fun verifyPin(pin: String): Boolean {
        return accessControlManager.verifyOwnerPin(pin)
    }

    /**
     * Observe store session status (open/closed).
     */
    private fun observeStoreSession() {
        viewModelScope.launch {
            storeSessionRepository.getActiveSession().collect { session ->
                _uiState.update { 
                    it.copy(
                        isStoreOpen = session != null,
                        openingBalance = session?.openingCash ?: 0L
                    ) 
                }
            }
        }
    }

    /**
     * Load all dashboard data.
     */
    fun refresh() {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Load dashboard stats
                val stats = reportRepository.getDashboardStats()
                
                // Load today's PnL for expenses
                val (startOfDay, endOfDay) = DateUtils.getTodayRange()
                val pnlReport = reportRepository.getProfitLossReport(startOfDay, endOfDay)
                val todayExpenses = pnlReport.totalExpenses
                
                // Load weekly revenue for chart
                val weeklyRevenue = reportRepository.getDailyRevenueHistory(7)
                
                // Load top products (last 30 days)
                val (startDate, endDate) = DateUtils.getLastNDaysRange(30)
                val topProducts = reportRepository.getTopSellingProducts(startDate, endDate, 5)
                
                // Calculate analytics
                val averageTransactions = calculateAverageTransactions(weeklyRevenue)
                val peakHours = calculatePeakHours()
                val averageOrderValue = calculateAverageOrderValue(stats)
                val avgItemsPerTransaction = calculateAverageItemsPerTransaction(stats)
                val busiestDay = calculateBusiestDay(weeklyRevenue)
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        stats = stats,
                        weeklyRevenue = weeklyRevenue,
                        topProducts = topProducts,
                        averageTransactionsPerDay = averageTransactions,
                        peakHours = peakHours,
                        averageOrderValue = averageOrderValue,
                        averageItemsPerTransaction = avgItemsPerTransaction,
                        growthPercent = stats.revenueGrowthPercent,
                        busiestDay = busiestDay,
                        todayExpenses = todayExpenses
                    )
                }
                
                Timber.d("Dashboard loaded: ${stats.todayTransactions} transactions today")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load dashboard")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Gagal memuat dashboard"
                    )
                }
            }
        }
    }

    /**
     * Open the store with the given opening cash amount.
     */
    fun openStore(openingCash: Long) {
        viewModelScope.launch {
            try {
                storeSessionRepository.openSession(openingCash)
                Timber.d("Store opened with opening cash: $openingCash")
            } catch (e: Exception) {
                Timber.e(e, "Failed to open store")
            }
        }
    }

    /**
     * Close the store.
     */
    fun closeStore() {
        viewModelScope.launch {
            try {
                val session = storeSessionRepository.getActiveSessionOneShot()
                session?.let {
                    storeSessionRepository.closeSession(it.id)
                    Timber.d("Store closed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to close store")
            }
        }
    }

    // Helper functions for analytics calculations

    private fun calculateAverageTransactions(weeklyRevenue: List<com.zatiaras.pos.feature.reports.domain.model.DailyRevenue>): Int {
        if (weeklyRevenue.isEmpty()) return 0
        // This is a simplified calculation - ideally we'd have transaction count per day
        return weeklyRevenue.size
    }

    private fun calculatePeakHours(): String {
        // Placeholder - would need hourly data to calculate this properly
        return "10:00 - 14:00"
    }

    private fun calculateAverageOrderValue(stats: com.zatiaras.pos.feature.reports.domain.model.DashboardStats): Long {
        if (stats.todayTransactions == 0) return 0L
        return stats.todayRevenue / stats.todayTransactions
    }

    private fun calculateAverageItemsPerTransaction(stats: com.zatiaras.pos.feature.reports.domain.model.DashboardStats): Double {
        if (stats.todayTransactions == 0) return 0.0
        return stats.todayItemsSold.toDouble() / stats.todayTransactions
    }

    private fun calculateBusiestDay(weeklyRevenue: List<com.zatiaras.pos.feature.reports.domain.model.DailyRevenue>): String {
        if (weeklyRevenue.isEmpty()) return "-"
        val busiestDay = weeklyRevenue.maxByOrNull { it.revenue }
        return busiestDay?.let {
            java.text.SimpleDateFormat("EEEE", java.util.Locale("id", "ID")).format(java.util.Date(it.date))
        } ?: "-"
    }
}

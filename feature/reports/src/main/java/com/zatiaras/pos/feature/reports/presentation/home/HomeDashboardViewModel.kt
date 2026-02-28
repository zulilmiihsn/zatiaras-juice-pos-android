package com.zatiaras.pos.feature.reports.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.access.AccessControlManager
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
import com.zatiaras.pos.core.domain.util.DateUtils
import com.zatiaras.pos.core.domain.util.LocaleUtils
import com.zatiaras.pos.feature.reports.domain.model.DailyRevenue
import com.zatiaras.pos.feature.reports.domain.model.DashboardStats
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import com.zatiaras.pos.feature.reports.domain.usecase.GenerateProfitLossReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.zatiaras.pos.feature.reports.R
import javax.inject.Inject

/**
 * ViewModel for Home Dashboard Screen.
 * Manages dashboard data including today's stats, top products, and store session.
 */
@HiltViewModel
class HomeDashboardViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val storeSessionRepository: StoreSessionRepository,
    private val accessControlManager: AccessControlManager,
    private val calculateDashboardAnalyticsUseCase: com.zatiaras.pos.feature.reports.domain.usecase.CalculateDashboardAnalyticsUseCase,
    private val generateProfitLossReportUseCase: GenerateProfitLossReportUseCase
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
                val (startOfDay, endOfDay) = DateUtils.getTodayRange()
                val (recentStart, recentEnd) = DateUtils.getLastNDaysRange(30)

                // Parallel data loading
                val statsDeferred = async { reportRepository.getDashboardStats() }
                val pnlDeferred = async { generateProfitLossReportUseCase(startOfDay, endOfDay) }
                val weeklyDeferred = async { reportRepository.getDailyRevenueHistory(7) }
                val topProductsDeferred = async { reportRepository.getTopSellingProducts(recentStart, recentEnd, 5) }

                val stats = statsDeferred.await().getOrThrow()
                val pnlReport = pnlDeferred.await().getOrThrow()
                val weeklyRevenue = weeklyDeferred.await().getOrThrow()
                val topProducts = topProductsDeferred.await().getOrThrow()
                
                val todayExpenses = pnlReport.totalExpenses
                
                // Calculate analytics on background thread
                val analyticsResult = withContext(Dispatchers.Default) {
                    calculateDashboardAnalyticsUseCase(stats, weeklyRevenue)
                }
                
                // Hide growth if no historical data exists
                val hasGrowthData = !(stats.weeklyRevenue == 0L && stats.revenueGrowthPercent == 0.0)
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        stats = stats,
                        weeklyRevenue = weeklyRevenue,
                        topProducts = topProducts,
                        averageTransactionsPerDay = analyticsResult.averageTransactionsPerDay,
                        peakHours = analyticsResult.peakHours,
                        averageOrderValue = analyticsResult.averageOrderValue,
                        averageItemsPerTransaction = analyticsResult.averageItemsPerTransaction,
                        growthPercent = if (hasGrowthData) stats.revenueGrowthPercent else null,
                        busiestDay = analyticsResult.busiestDay,
                        todayExpenses = todayExpenses
                    )
                }
                
                Timber.d("Dashboard loaded: ${stats.todayTransactions} transactions today")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load dashboard")
                val errorMessage = e.message ?: ""
                val uiError = if (errorMessage.isNotBlank()) {
                    com.zatiaras.pos.core.ui.util.UiText.DynamicString(errorMessage)
                } else {
                    com.zatiaras.pos.core.ui.util.UiText.StringResource(R.string.reports_error_load)
                }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = uiError
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

}

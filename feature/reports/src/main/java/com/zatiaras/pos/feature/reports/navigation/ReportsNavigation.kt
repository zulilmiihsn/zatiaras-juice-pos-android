package com.zatiaras.pos.feature.reports.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.zatiaras.pos.feature.reports.presentation.dashboard.ReportDashboardRoute

const val REPORTS_ROUTE = "reports"
const val REPORT_DASHBOARD_ROUTE = "reports/dashboard"

fun NavController.navigateToReports(navOptions: NavOptions? = null) {
    navigate(REPORT_DASHBOARD_ROUTE, navOptions)
}

fun NavGraphBuilder.reportsScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = REPORT_DASHBOARD_ROUTE) {
        ReportDashboardRoute(
            onNavigateBack = onNavigateBack
        )
    }
}

package com.zatiaras.pos.feature.reports.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.zatiaras.pos.feature.reports.presentation.dashboard.ReportDashboardRoute
import com.zatiaras.pos.feature.reports.presentation.pnl.PnlReportRoute

const val REPORTS_ROUTE = "reports"
const val REPORT_DASHBOARD_ROUTE = "reports/dashboard"
const val PNL_REPORT_ROUTE = "reports/pnl"

fun NavController.navigateToReports(navOptions: NavOptions? = null) {
    navigate(REPORT_DASHBOARD_ROUTE, navOptions)
}

fun NavController.navigateToPnlReport(navOptions: NavOptions? = null) {
    navigate(PNL_REPORT_ROUTE, navOptions)
}

fun NavGraphBuilder.reportsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPnl: () -> Unit = {}
) {
    composable(route = REPORT_DASHBOARD_ROUTE) {
        ReportDashboardRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToPnl = onNavigateToPnl
        )
    }
}

fun NavGraphBuilder.pnlReportScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = PNL_REPORT_ROUTE) {
        PnlReportRoute(
            onNavigateBack = onNavigateBack
        )
    }
}


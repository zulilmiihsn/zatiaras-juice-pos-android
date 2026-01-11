package com.zatiaras.pos.feature.reports.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.zatiaras.pos.feature.reports.presentation.dashboard.ReportDashboardRoute
import com.zatiaras.pos.feature.reports.presentation.home.HomeDashboardRoute
import com.zatiaras.pos.feature.reports.presentation.pnl.PnlReportRoute

const val HOME_DASHBOARD_ROUTE = "home"
const val REPORTS_ROUTE = "reports"
const val REPORT_DASHBOARD_ROUTE = "reports/dashboard"
const val PNL_REPORT_ROUTE = "reports/pnl"

fun NavController.navigateToReports(navOptions: NavOptions? = null) {
    navigate(REPORT_DASHBOARD_ROUTE, navOptions)
}

fun NavController.navigateToPnlReport(navOptions: NavOptions? = null) {
    navigate(PNL_REPORT_ROUTE, navOptions)
}

/**
 * Home Dashboard Screen (Tab "Beranda")
 * Quick overview with top metrics
 */
fun NavGraphBuilder.homeDashboardScreen(
    route: String = HOME_DASHBOARD_ROUTE,
    onNavigateToSettings: () -> Unit = {}
) {
    composable(route = route) {
        HomeDashboardRoute(
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

/**
 * Reports Dashboard Screen (Tab "Laporan")
 * Detailed reports with charts and P&L access
 */
fun NavGraphBuilder.reportsScreen(
    route: String = REPORT_DASHBOARD_ROUTE,
    onNavigateBack: (() -> Unit)?,
    onNavigateToPnl: () -> Unit = {}
) {
    composable(route = route) {
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

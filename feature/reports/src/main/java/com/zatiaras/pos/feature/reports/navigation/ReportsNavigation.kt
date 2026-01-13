package com.zatiaras.pos.feature.reports.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.zatiaras.pos.core.data.access.AccessControlManager
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
 * Detailed reports with charts and P&L access.
 * Protected by Access Control if manager is provided.
 */
fun NavGraphBuilder.reportsScreen(
    route: String = REPORT_DASHBOARD_ROUTE,
    onNavigateBack: (() -> Unit)?,
    onNavigateToPnl: () -> Unit = {},
    accessControlManager: com.zatiaras.pos.core.data.access.AccessControlManager? = null
) {
    composable(route = route) {
        if (accessControlManager != null) {
            com.zatiaras.pos.core.ui.components.AccessControlGate(
                accessControlManager = accessControlManager,
                route = com.zatiaras.pos.core.data.access.LockableRoute.REPORTS_TAB.route,
                screenName = "Laporan",
                onAccessDenied = { onNavigateBack?.invoke() }
            ) {
                ReportDashboardRoute(
                    onNavigateBack = onNavigateBack,
                    onNavigateToPnl = onNavigateToPnl
                )
            }
        } else {
            ReportDashboardRoute(
                onNavigateBack = onNavigateBack,
                onNavigateToPnl = onNavigateToPnl
            )
        }
    }
}

/**
 * P&L Report Screen with access control.
 * If accessControlManager is provided, kasir will need to enter owner PIN if locked.
 */
fun NavGraphBuilder.pnlReportScreen(
    onNavigateBack: () -> Unit,
    accessControlManager: AccessControlManager? = null
) {
    composable(route = PNL_REPORT_ROUTE) {
        if (accessControlManager != null) {
            PnlReportRoute(
                onNavigateBack = onNavigateBack,
                accessControlManager = accessControlManager
            )
        } else {
            PnlReportRoute(
                onNavigateBack = onNavigateBack
            )
        }
    }
}

package com.zatiaras.pos.feature.reports.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.zatiaras.pos.core.data.access.AccessControlManager
import com.zatiaras.pos.core.data.access.LockableRoute
import com.zatiaras.pos.core.ui.components.AccessControlGate
import com.zatiaras.pos.feature.reports.presentation.home.HomeDashboardRoute
import com.zatiaras.pos.feature.reports.presentation.pnl.PnlReportRoute

const val HOME_DASHBOARD_ROUTE = "home"
const val REPORTS_ROUTE = "reports"
const val PNL_REPORT_ROUTE = "reports/pnl"
const val REPORT_CHAT_ROUTE = "reports/chat"

fun NavController.navigateToReports(navOptions: NavOptions? = null) {
    navigate(REPORTS_ROUTE, navOptions)
}

fun NavController.navigateToPnlReport(navOptions: NavOptions? = null) {
    navigate(PNL_REPORT_ROUTE, navOptions)
}

fun NavController.navigateToReportChat(navOptions: NavOptions? = null) {
    navigate(REPORT_CHAT_ROUTE, navOptions)
}

/**
 * Home Dashboard Screen (Tab "Beranda")
 * Complete business overview with stats, charts, period summary, and top products.
 * Content from the old ReportDashboardScreen is now shown here.
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
 * Reports Screen (Tab "Laporan")
 * Shows P&L Report directly with period selector, breakdown, and export options.
 * Includes "Tanya AI" floating action button.
 * Back button is hidden since this is a bottom nav tab.
 * Protected by Access Control if manager is provided.
 */
fun NavGraphBuilder.reportsScreen(
    route: String = REPORTS_ROUTE,
    onNavigateBack: (() -> Unit)?,
    onNavigateToChat: () -> Unit = {},
    accessControlManager: AccessControlManager? = null
) {
    composable(route = route) {
        if (accessControlManager != null) {
            AccessControlGate(
                accessControlManager = accessControlManager,
                route = LockableRoute.REPORTS_TAB.route,
                screenName = "Laporan",
                onAccessDenied = { onNavigateBack?.invoke() }
            ) {
                // Pass null for onNavigateBack to hide back button in tab view
                PnlReportRoute(
                    onNavigateBack = null,
                    onNavigateToChat = onNavigateToChat
                )
            }
        } else {
            // Pass null for onNavigateBack to hide back button in tab view
            PnlReportRoute(
                onNavigateBack = null,
                onNavigateToChat = onNavigateToChat
            )
        }
    }
}

/**
 * Standalone P&L Report Screen (for deep linking or external navigation)
 * If accessControlManager is provided, kasir will need to enter owner PIN if locked.
 */
fun NavGraphBuilder.pnlReportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    accessControlManager: AccessControlManager? = null
) {
    composable(route = PNL_REPORT_ROUTE) {
        if (accessControlManager != null) {
            PnlReportRoute(
                onNavigateBack = onNavigateBack,
                onNavigateToChat = onNavigateToChat,
                accessControlManager = accessControlManager
            )
        } else {
            PnlReportRoute(
                onNavigateBack = onNavigateBack,
                onNavigateToChat = onNavigateToChat
            )
        }
    }
}

/**
 * AI Chat Assistant Screen
 */
fun NavGraphBuilder.reportChatScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = REPORT_CHAT_ROUTE) {
        com.zatiaras.pos.feature.reports.presentation.chat.ReportChatRoute(
            onNavigateBack = onNavigateBack
        )
    }
}

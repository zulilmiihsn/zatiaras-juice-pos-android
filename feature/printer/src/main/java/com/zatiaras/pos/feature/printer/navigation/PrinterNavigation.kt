package com.zatiaras.pos.feature.printer.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.zatiaras.pos.feature.printer.presentation.PrinterSettingsRoute

/**
 * Navigation routes for printer feature.
 */
object PrinterRoutes {
    const val PRINTER_SETTINGS = "printer_settings"
}

/**
 * Navigate to printer settings screen.
 */
fun NavController.navigateToPrinterSettings() {
    navigate(PrinterRoutes.PRINTER_SETTINGS)
}

/**
 * Add printer settings screen to navigation graph.
 * Protected by Access Control if manager is provided.
 */
fun NavGraphBuilder.printerSettingsScreen(
    onNavigateBack: () -> Unit,
    accessControlManager: com.zatiaras.pos.core.data.access.AccessControlManager? = null
) {
    composable(PrinterRoutes.PRINTER_SETTINGS) {
        if (accessControlManager != null) {
            com.zatiaras.pos.core.ui.components.AccessControlGate(
                accessControlManager = accessControlManager,
                route = com.zatiaras.pos.core.data.access.LockableRoute.PRINTER_SETTINGS.route,
                screenName = "Pengaturan Printer",
                onAccessDenied = onNavigateBack
            ) {
                PrinterSettingsRoute(
                    onNavigateBack = onNavigateBack
                )
            }
        } else {
            PrinterSettingsRoute(
                onNavigateBack = onNavigateBack
            )
        }
    }
}

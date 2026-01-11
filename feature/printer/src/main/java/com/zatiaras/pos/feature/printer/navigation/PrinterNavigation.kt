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
 */
fun NavGraphBuilder.printerSettingsScreen(
    onNavigateBack: () -> Unit
) {
    composable(PrinterRoutes.PRINTER_SETTINGS) {
        PrinterSettingsRoute(
            onNavigateBack = onNavigateBack
        )
    }
}

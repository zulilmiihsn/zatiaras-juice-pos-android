package com.zatiaras.pos.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.zatiaras.pos.feature.auth.LoginRoute
import com.zatiaras.pos.feature.auth.lock.AppLockRoute
import com.zatiaras.pos.feature.auth.lock.PinSetupRoute
import com.zatiaras.pos.feature.auth.settings.SettingsRoute

/**
 * Navigation routes for the Auth feature module.
 */
object AuthRoutes {
    const val LOGIN = "auth/login"
    const val APP_LOCK = "auth/app_lock"
    const val SETTINGS = "auth/settings"
    const val PIN_SETUP = "auth/pin_setup"
    const val PIN_CHANGE = "auth/pin_change"
}

/**
 * Add Login screen to navigation graph.
 */
fun NavGraphBuilder.loginScreen(
    onLoginSuccess: () -> Unit
) {
    composable(AuthRoutes.LOGIN) {
        LoginRoute(onLoginSuccess = onLoginSuccess)
    }
}

/**
 * Add App Lock screen to navigation graph.
 */
fun NavGraphBuilder.appLockScreen(
    onUnlocked: () -> Unit
) {
    composable(AuthRoutes.APP_LOCK) {
        AppLockRoute(onUnlocked = onUnlocked)
    }
}

/**
 * Add Settings screen to navigation graph.
 */
fun NavGraphBuilder.settingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPinSetup: () -> Unit,
    onNavigateToPrinter: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onLogout: () -> Unit
) {
    composable(AuthRoutes.SETTINGS) {
        SettingsRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToPinSetup = onNavigateToPinSetup,
            onNavigateToPrinter = onNavigateToPrinter,
            onNavigateToInventory = onNavigateToInventory,
            onLogout = onLogout
        )
    }
}

/**
 * Add PIN Setup screen to navigation graph.
 */
fun NavGraphBuilder.pinSetupScreen(
    onPinSet: () -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(AuthRoutes.PIN_SETUP) {
        PinSetupRoute(
            isChangingPin = false,
            onPinSet = onPinSet,
            onNavigateBack = onNavigateBack
        )
    }
}

/**
 * Add PIN Change screen to navigation graph.
 */
fun NavGraphBuilder.pinChangeScreen(
    onPinSet: () -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(AuthRoutes.PIN_CHANGE) {
        PinSetupRoute(
            isChangingPin = true,
            onPinSet = onPinSet,
            onNavigateBack = onNavigateBack
        )
    }
}

// ==================== Navigation Extensions ====================

fun NavController.navigateToLogin() {
    navigate(AuthRoutes.LOGIN) {
        popUpTo(0) { inclusive = true }
    }
}

fun NavController.navigateToAppLock() {
    navigate(AuthRoutes.APP_LOCK) {
        popUpTo(0) { inclusive = true }
    }
}

fun NavController.navigateToSettings() {
    navigate(AuthRoutes.SETTINGS)
}

fun NavController.navigateToPinSetup() {
    navigate(AuthRoutes.PIN_SETUP)
}

fun NavController.navigateToPinChange() {
    navigate(AuthRoutes.PIN_CHANGE)
}

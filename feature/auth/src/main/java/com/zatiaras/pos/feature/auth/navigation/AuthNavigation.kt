package com.zatiaras.pos.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.zatiaras.pos.feature.auth.LoginRoute
import com.zatiaras.pos.feature.auth.lock.AppLockRoute
import com.zatiaras.pos.feature.auth.lock.PinSetupRoute
import com.zatiaras.pos.feature.auth.settings.SettingsRoute
import com.zatiaras.pos.feature.auth.settings.SecuritySettingsScreen
import com.zatiaras.pos.feature.auth.settings.AccessControlScreen
import com.zatiaras.pos.feature.auth.settings.OwnerPinSetupScreen
import com.zatiaras.pos.feature.auth.settings.SyncSettingsScreen
import com.zatiaras.pos.feature.auth.settings.AboutScreen

/**
 * Navigation routes for the Auth feature module.
 */
object AuthRoutes {
    const val LOGIN = "auth/login"
    const val APP_LOCK = "auth/app_lock"
    const val SETTINGS = "auth/settings"
    const val SETTINGS_SECURITY = "auth/settings/security"
    const val SETTINGS_ACCESS_CONTROL = "auth/settings/access_control"
    const val SETTINGS_SYNC = "auth/settings/sync"
    const val SETTINGS_ABOUT = "auth/settings/about"
    const val PIN_SETUP = "auth/pin_setup"
    const val OWNER_PIN_SETUP = "auth/owner_pin_setup"
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
 * Protected by Access Control.
 */
fun NavGraphBuilder.settingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrinter: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToAccessControl: () -> Unit = {},
    onNavigateToTransactionHistory: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onLogout: () -> Unit,
    accessControlManager: com.zatiaras.pos.core.data.access.AccessControlManager? = null
) {
    composable(AuthRoutes.SETTINGS) {
        if (accessControlManager != null) {
            com.zatiaras.pos.core.ui.components.AccessControlGate(
                accessControlManager = accessControlManager,
                route = com.zatiaras.pos.core.data.access.LockableRoute.SETTINGS.route,
                screenName = "Pengaturan",
                onAccessDenied = onNavigateBack
            ) {
                SettingsRoute(
                    onNavigateBack = onNavigateBack,
                    onNavigateToPrinter = onNavigateToPrinter,
                    onNavigateToInventory = onNavigateToInventory,
                    onNavigateToSecurity = onNavigateToSecurity,
                    onNavigateToAccessControl = onNavigateToAccessControl,
                    onNavigateToTransactionHistory = onNavigateToTransactionHistory,
                    onNavigateToSync = onNavigateToSync,
                    onNavigateToAbout = onNavigateToAbout,
                    onLogout = onLogout
                )
            }
        } else {
            SettingsRoute(
                onNavigateBack = onNavigateBack,
                onNavigateToPrinter = onNavigateToPrinter,
                onNavigateToInventory = onNavigateToInventory,
                onNavigateToSecurity = onNavigateToSecurity,
                onNavigateToAccessControl = onNavigateToAccessControl,
                onNavigateToTransactionHistory = onNavigateToTransactionHistory,
                onNavigateToSync = onNavigateToSync,
                onNavigateToAbout = onNavigateToAbout,
                onLogout = onLogout
            )
        }
    }
}

/**
 * Add Security Settings sub-screen to navigation graph.
 */
fun NavGraphBuilder.securitySettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPinSetup: () -> Unit
) {
    composable(AuthRoutes.SETTINGS_SECURITY) {
        SecuritySettingsScreen(
            onNavigateBack = onNavigateBack,
            onChangePinClick = onNavigateToPinSetup
        )
    }
}

/**
 * Add Access Control sub-screen to navigation graph.
 */
fun NavGraphBuilder.accessControlScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOwnerPinSetup: () -> Unit
) {
    composable(AuthRoutes.SETTINGS_ACCESS_CONTROL) {
        AccessControlScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToOwnerPinSetup = onNavigateToOwnerPinSetup
        )
    }
}

/**
 * Add Sync Settings sub-screen to navigation graph.
 */
fun NavGraphBuilder.syncSettingsScreen(
    onNavigateBack: () -> Unit
) {
    composable(AuthRoutes.SETTINGS_SYNC) {
        SyncSettingsScreen(
            onNavigateBack = onNavigateBack
        )
    }
}

/**
 * Add About sub-screen to navigation graph.
 */
fun NavGraphBuilder.aboutScreen(
    onNavigateBack: () -> Unit
) {
    composable(AuthRoutes.SETTINGS_ABOUT) {
        AboutScreen(
            onNavigateBack = onNavigateBack
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
 * Add Owner PIN Setup screen to navigation graph.
 */
fun NavGraphBuilder.ownerPinSetupScreen(
    onPinSet: () -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(AuthRoutes.OWNER_PIN_SETUP) {
        OwnerPinSetupScreen(
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

fun NavController.navigateToSecuritySettings() {
    navigate(AuthRoutes.SETTINGS_SECURITY)
}

fun NavController.navigateToAccessControl() {
    navigate(AuthRoutes.SETTINGS_ACCESS_CONTROL)
}

fun NavController.navigateToSyncSettings() {
    navigate(AuthRoutes.SETTINGS_SYNC)
}

fun NavController.navigateToAbout() {
    navigate(AuthRoutes.SETTINGS_ABOUT)
}

fun NavController.navigateToPinSetup() {
    navigate(AuthRoutes.PIN_SETUP)
}

fun NavController.navigateToOwnerPinSetup() {
    navigate(AuthRoutes.OWNER_PIN_SETUP)
}

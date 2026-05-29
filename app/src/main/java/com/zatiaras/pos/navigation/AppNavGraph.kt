package com.zatiaras.pos.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.zatiaras.pos.NavRoutes
import com.zatiaras.pos.app.MainScreen
import com.zatiaras.pos.core.data.access.AccessControlManager
import com.zatiaras.pos.feature.auth.LoginRoute
import com.zatiaras.pos.feature.auth.lock.AppLockRoute
import com.zatiaras.pos.feature.auth.navigation.AuthRoutes
import com.zatiaras.pos.feature.auth.navigation.aboutScreen
import com.zatiaras.pos.feature.auth.navigation.accessControlScreen
import com.zatiaras.pos.feature.auth.navigation.ownerPinSetupScreen
import com.zatiaras.pos.feature.auth.navigation.pinSetupScreen
import com.zatiaras.pos.feature.auth.navigation.securitySettingsScreen
import com.zatiaras.pos.feature.auth.navigation.settingsScreen
import com.zatiaras.pos.feature.auth.navigation.syncSettingsScreen
import com.zatiaras.pos.feature.inventory.navigation.inventoryNavGraph
import com.zatiaras.pos.feature.inventory.navigation.navigateToInventory
import com.zatiaras.pos.feature.pos.domain.model.CartHolder
import com.zatiaras.pos.feature.pos.domain.model.TransactionHolder
import com.zatiaras.pos.feature.pos.navigation.PosRoutes
import com.zatiaras.pos.feature.pos.navigation.checkoutScreen
import com.zatiaras.pos.feature.pos.navigation.navigateToCheckout
import com.zatiaras.pos.feature.pos.navigation.transactionHistoryScreen
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptEvent
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptScreen
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptViewModel
import com.zatiaras.pos.feature.printer.navigation.navigateToPrinterSettings
import com.zatiaras.pos.feature.printer.navigation.printerSettingsScreen
import com.zatiaras.pos.feature.reports.navigation.navigateToReportChat
import com.zatiaras.pos.feature.reports.navigation.pnlReportScreen
import com.zatiaras.pos.feature.reports.navigation.reportChatScreen

/**
 * Main navigation graph for the ZatiarasPOS app.
 *
 * Keep app-wide holders and protected feature entry points wired here. Feature
 * modules own their internal graphs; this file should only describe how those
 * top-level surfaces connect.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    cartHolder: CartHolder,
    transactionHolder: TransactionHolder,
    accessControlManager: AccessControlManager,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = NavRoutes.STARTUP,
        modifier = modifier,
    ) {
        // Startup is a transient route: it decides whether to restore a session,
        // require PIN unlock, or fall through to login.
        composable(NavRoutes.STARTUP) {
            val viewModel: StartupViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(state) {
                when (state) {
                    is StartupState.SessionRestored -> {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.STARTUP) { inclusive = true }
                        }
                    }
                    is StartupState.NeedsUnlock -> {
                        navController.navigate(NavRoutes.APP_LOCK) {
                            popUpTo(NavRoutes.STARTUP) { inclusive = true }
                        }
                    }
                    is StartupState.NeedsLogin,
                    is StartupState.SessionExpired,
                    -> {
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(NavRoutes.STARTUP) { inclusive = true }
                        }
                    }
                    is StartupState.Loading -> {
                        // Stay on splash while the ViewModel resolves storage.
                    }
                }
            }

            SplashScreen()
        }

        // App lock only appears after a valid restored session requires local
        // unlock. Login should not be repeated for this path.
        composable(NavRoutes.APP_LOCK) {
            AppLockRoute(
                onUnlocked = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.APP_LOCK) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoutes.LOGIN) {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoutes.HOME) {
            MainScreen(
                cartHolder = cartHolder,
                onNavigateBackFromMain = {
                    // Home is the root after login; back handling is owned by
                    // the Activity/system rather than the nested graph.
                },
                onNavigateToCheckout = {
                    navController.navigateToCheckout()
                },
                onNavigateToChat = {
                    navController.navigateToReportChat()
                },
                onNavigateToReceipt = { transaction ->
                    transactionHolder.setTransaction(transaction)
                    navController.navigate(NavRoutes.RECEIPT)
                },
                onNavigateToSettings = {
                    navController.navigate(AuthRoutes.SETTINGS)
                },
                accessControlManager = accessControlManager,
            )
        }

        inventoryNavGraph(navController, accessControlManager)

        // Checkout receives the current cart via CartHolder because the route
        // itself has no stable serializable cart argument.
        checkoutScreen(
            cartHolder = cartHolder,
            onNavigateBack = {
                navController.popBackStack()
            },
            onTransactionComplete = { transaction ->
                transactionHolder.setTransaction(transaction)
                navController.navigate(NavRoutes.RECEIPT) {
                    popUpTo(PosRoutes.POS) { inclusive = false }
                }
            },
        )

        // Settings fans out to protected operational screens.
        settingsScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToPrinter = {
                navController.navigateToPrinterSettings()
            },
            onNavigateToInventory = {
                navController.navigateToInventory()
            },
            onNavigateToSecurity = {
                navController.navigate(AuthRoutes.SETTINGS_SECURITY)
            },
            onNavigateToAccessControl = {
                navController.navigate(AuthRoutes.SETTINGS_ACCESS_CONTROL)
            },
            onNavigateToTransactionHistory = {
                navController.navigate(com.zatiaras.pos.feature.pos.navigation.PosRoutes.TRANSACTION_HISTORY)
            },
            onNavigateToSync = {
                navController.navigate(AuthRoutes.SETTINGS_SYNC)
            },
            onNavigateToAbout = {
                navController.navigate(AuthRoutes.SETTINGS_ABOUT)
            },
            onLogout = {
                navController.navigate(NavRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            },
            accessControlManager = accessControlManager,
        )

        securitySettingsScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToPinSetup = {
                navController.navigate(AuthRoutes.PIN_SETUP)
            },
        )

        accessControlScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToOwnerPinSetup = {
                navController.navigate(AuthRoutes.OWNER_PIN_SETUP)
            },
        )

        syncSettingsScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
        )

        aboutScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
        )

        // P&L can be opened directly and still enforces access control inside
        // the reports navigation entry.
        pnlReportScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToChat = {
                navController.navigateToReportChat()
            },
            accessControlManager = accessControlManager,
        )

        reportChatScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
        )

        // Receipt navigation uses TransactionHolder for the selected history
        // transaction to avoid oversized navigation arguments.
        transactionHistoryScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToReceipt = { transaction ->
                transactionHolder.setTransaction(transaction)
                navController.navigate(NavRoutes.RECEIPT)
            },
        )

        pinSetupScreen(
            onPinSet = {
                navController.popBackStack()
            },
            onNavigateBack = {
                navController.popBackStack()
            },
        )

        ownerPinSetupScreen(
            onPinSet = {
                navController.popBackStack()
            },
            onNavigateBack = {
                navController.popBackStack()
            },
        )

        printerSettingsScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            accessControlManager = accessControlManager,
        )

        composable(NavRoutes.RECEIPT) {
            ReceiptRoute(
                transactionHolder = transactionHolder,
                navController = navController,
                context = context,
            )
        }
    }
}

/**
 * Bridges receipt navigation holder state into ReceiptViewModel state.
 *
 * The ViewModel remains the primary source of truth after hydration; the holder
 * is consumed only for cross-graph navigation.
 */
@Composable
private fun ReceiptRoute(
    transactionHolder: TransactionHolder,
    navController: NavHostController,
    context: Context,
) {
    val receiptViewModel: ReceiptViewModel = hiltViewModel()
    val receiptUiState by receiptViewModel.uiState.collectAsStateWithLifecycle()

    // Primary source of truth is the ViewModel's state after hydration.
    val transaction = receiptUiState.transaction

    // Consume the holder once so process-local navigation state cannot replay
    // stale receipts after this screen is recreated.
    LaunchedEffect(Unit) {
        if (receiptViewModel.uiState.value.transaction == null) {
            transactionHolder.consumeTransaction()?.let {
                receiptViewModel.setTransaction(it)
            }
        }
    }

    // Keep Android side effects in the route; ReceiptViewModel emits plain
    // events that this boundary translates to toast/navigation.
    LaunchedEffect(Unit) {
        receiptViewModel.events.collect { event ->
            when (event) {
                is ReceiptEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is ReceiptEvent.PrintSuccess -> {
                    // Print success also emits ShowToast, so no navigation is
                    // needed for this event.
                }
                is ReceiptEvent.NavigateToPrinterSettings -> {
                    navController.navigateToPrinterSettings()
                }
            }
        }
    }

    if (transaction != null) {
        ReceiptScreen(
            transaction = transaction,
            onNavigateBack = {
                navController.popBackStack()
            },
            onNewTransaction = {
                // Home owns the POS tab. Returning there resets the receipt
                // flow without constructing a POS route manually.
                navController.navigate(NavRoutes.HOME) {
                    popUpTo(NavRoutes.HOME) { inclusive = true }
                }
            },
            onPrintReceipt = {
                if (receiptUiState.isPrinterConnected) {
                    receiptViewModel.printReceipt()
                } else {
                    navController.navigateToPrinterSettings()
                }
            },
            isPrinting = receiptUiState.isPrinting,
            isPrinterConnected = receiptUiState.isPrinterConnected,
            printerName = receiptUiState.printerName,
        )
    } else {
        // Fallback while the holder is consumed or if a stale receipt route is
        // opened without transaction state.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

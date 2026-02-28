package com.zatiaras.pos.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.zatiaras.pos.feature.auth.navigation.pinSetupScreen
import com.zatiaras.pos.feature.auth.navigation.settingsScreen
import com.zatiaras.pos.feature.auth.navigation.securitySettingsScreen
import com.zatiaras.pos.feature.pos.navigation.transactionHistoryScreen
import com.zatiaras.pos.feature.auth.navigation.accessControlScreen
import com.zatiaras.pos.feature.auth.navigation.ownerPinSetupScreen
import com.zatiaras.pos.feature.auth.navigation.syncSettingsScreen
import com.zatiaras.pos.feature.auth.navigation.aboutScreen
import com.zatiaras.pos.feature.inventory.navigation.inventoryNavGraph
import com.zatiaras.pos.feature.inventory.navigation.navigateToInventory
import com.zatiaras.pos.feature.pos.domain.model.CartHolder
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.model.TransactionHolder
import com.zatiaras.pos.feature.pos.navigation.PosRoutes
import com.zatiaras.pos.feature.pos.navigation.checkoutScreen
import com.zatiaras.pos.feature.pos.navigation.navigateToCheckout
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptEvent
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptScreen
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptViewModel
import com.zatiaras.pos.feature.printer.navigation.navigateToPrinterSettings
import com.zatiaras.pos.feature.printer.navigation.printerSettingsScreen
import com.zatiaras.pos.feature.reports.navigation.navigateToPnlReport
import com.zatiaras.pos.feature.reports.navigation.navigateToReportChat
import com.zatiaras.pos.feature.reports.navigation.pnlReportScreen
import com.zatiaras.pos.feature.reports.navigation.reportChatScreen

/**
 * Main navigation graph for the ZatiarasPOS app.
 * 
 * Extracted from MainActivity to follow KISS principle (Activity under 100 lines).
 * Contains all top-level navigation routes.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    cartHolder: CartHolder,
    transactionHolder: TransactionHolder,
    accessControlManager: AccessControlManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    NavHost(
        navController = navController,
        startDestination = NavRoutes.STARTUP,
        modifier = modifier
    ) {
        // Startup screen - checks for saved session and app lock
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
                        // Session is valid but app lock is enabled
                        navController.navigate(NavRoutes.APP_LOCK) {
                            popUpTo(NavRoutes.STARTUP) { inclusive = true }
                        }
                    }
                    is StartupState.NeedsLogin,
                    is StartupState.SessionExpired -> {
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(NavRoutes.STARTUP) { inclusive = true }
                        }
                    }
                    is StartupState.Loading -> {
                        // Stay on splash screen
                    }
                }
            }
            
            // Animated splash screen with logo
            SplashScreen()
        }
        
        // App Lock screen - shown when session is valid but lock is enabled
        composable(NavRoutes.APP_LOCK) {
            AppLockRoute(
                onUnlocked = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.APP_LOCK) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavRoutes.LOGIN) {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavRoutes.HOME) {
            MainScreen(
                cartHolder = cartHolder,
                onNavigateBackFromMain = {
                    // Handle back from main screen (e.g. minimize or double back to exit)
                    // For now, do nothing - Home is the end.
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
                accessControlManager = accessControlManager
            )
        }
        
        // Inventory feature navigation graph
        inventoryNavGraph(navController, accessControlManager)
        
        // Checkout (Full Screen)
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
            }
        )
        
        // Settings (Full Screen)
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
            accessControlManager = accessControlManager
        )
        
        // Security Settings Sub-Screen
        securitySettingsScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToPinSetup = {
                navController.navigate(AuthRoutes.PIN_SETUP)
            }
        )
        
        // Access Control Sub-Screen
        accessControlScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToOwnerPinSetup = {
                navController.navigate(AuthRoutes.OWNER_PIN_SETUP)
            }
        )
        
        // Sync Settings Sub-Screen
        syncSettingsScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
        
        // About Sub-Screen
        aboutScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
        
        // Reports P&L (Full Screen) - For deep linking, protected by Access Control
        pnlReportScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToChat = {
                navController.navigateToReportChat()
            },
            accessControlManager = accessControlManager
        )
        
        // Reports AI Chat (Full Screen)
        reportChatScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )

        // Transaction History (Full Screen)
        transactionHistoryScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToReceipt = { transaction ->
                transactionHolder.setTransaction(transaction)
                navController.navigate(NavRoutes.RECEIPT)
            }
        )
        
        // Pin Setup
        pinSetupScreen(
            onPinSet = {
                navController.popBackStack()
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
        
        // Owner Pin Setup
        ownerPinSetupScreen(
            onPinSet = {
                navController.popBackStack()
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
        
        // Printer Settings - Protected by Access Control
        printerSettingsScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            accessControlManager = accessControlManager
        )
        
        // Receipt screen
        composable(NavRoutes.RECEIPT) {
            ReceiptRoute(
                transactionHolder = transactionHolder,
                navController = navController,
                context = context
            )
        }
    }
}

/**
 * Receipt route extracted to keep AppNavGraph clean.
 */
@Composable
private fun ReceiptRoute(
    transactionHolder: TransactionHolder,
    navController: NavHostController,
    context: Context
) {
    val receiptViewModel: ReceiptViewModel = hiltViewModel()
    val receiptUiState by receiptViewModel.uiState.collectAsStateWithLifecycle()
    
    // Primary source of truth is the ViewModel's state
    val transaction = receiptUiState.transaction
    
    // Only attempt to consume from holder if we don't have a transaction in ViewModel yet
    LaunchedEffect(Unit) {
        if (receiptViewModel.uiState.value.transaction == null) {
            transactionHolder.consumeTransaction()?.let {
                receiptViewModel.setTransaction(it)
            }
        }
    }
    
    // Handle receipt events
    LaunchedEffect(Unit) {
        receiptViewModel.events.collect { event ->
            when (event) {
                is ReceiptEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is ReceiptEvent.PrintSuccess -> {
                    // Print success handled in ShowToast
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
                // Navigate to HOME which contains the MainScreen with tabs
                // POS is a tab inside MainScreen's nested NavHost
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
            printerName = receiptUiState.printerName
        )
    } else {
        // Loading or fallback
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

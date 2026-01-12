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
import com.zatiaras.pos.feature.auth.LoginRoute
import com.zatiaras.pos.feature.auth.navigation.AuthRoutes
import com.zatiaras.pos.feature.auth.navigation.pinSetupScreen
import com.zatiaras.pos.feature.auth.navigation.settingsScreen
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
import com.zatiaras.pos.feature.reports.navigation.pnlReportScreen

/**
 * Main navigation graph for the Zatiaras POS app.
 * 
 * Extracted from MainActivity to follow KISS principle (Activity under 100 lines).
 * Contains all top-level navigation routes.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    cartHolder: CartHolder,
    transactionHolder: TransactionHolder,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    NavHost(
        navController = navController,
        startDestination = NavRoutes.STARTUP,
        modifier = modifier
    ) {
        // Startup screen - checks for saved session
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
            
            // Simple loading indicator
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
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
                onNavigateToPnl = {
                    navController.navigateToPnlReport()
                },
                onNavigateToSettings = {
                    navController.navigate(AuthRoutes.SETTINGS)
                }
            )
        }
        
        // Inventory feature navigation graph
        inventoryNavGraph(navController)
        
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
            onNavigateToPinSetup = {
                navController.navigate(AuthRoutes.PIN_SETUP)
            },
            onNavigateToPrinter = {
                navController.navigateToPrinterSettings()
            },
            onNavigateToInventory = {
                navController.navigateToInventory()
            },
            onLogout = {
                navController.navigate(NavRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
        
        // Reports P&L (Full Screen)
        pnlReportScreen(
            onNavigateBack = {
                navController.popBackStack()
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
        
        // Printer Settings
        printerSettingsScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
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
    
    var transaction by remember { mutableStateOf<Transaction?>(null) }
    
    LaunchedEffect(Unit) {
        transaction = transactionHolder.consumeTransaction()
        transaction?.let { receiptViewModel.setTransaction(it) }
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
            transaction = transaction!!,
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

package com.zatiaras.pos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zatiaras.pos.core.ui.theme.ZatiarasPOSTheme
import com.zatiaras.pos.app.MainScreen
import com.zatiaras.pos.feature.auth.LoginRoute
import com.zatiaras.pos.feature.auth.navigation.AuthRoutes
import com.zatiaras.pos.feature.auth.navigation.pinSetupScreen
import com.zatiaras.pos.feature.auth.navigation.settingsScreen
import com.zatiaras.pos.feature.inventory.navigation.InventoryNavigation
import com.zatiaras.pos.feature.inventory.navigation.inventoryNavGraph
import com.zatiaras.pos.feature.pos.domain.model.CartHolder
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.model.TransactionHolder
import com.zatiaras.pos.feature.pos.navigation.PosRoutes
import com.zatiaras.pos.feature.pos.navigation.cashRecordScreen
import com.zatiaras.pos.feature.pos.navigation.checkoutScreen
import com.zatiaras.pos.feature.pos.navigation.navigateToCashRecord
import com.zatiaras.pos.feature.pos.navigation.navigateToCheckout
import com.zatiaras.pos.feature.pos.navigation.navigateToPos
import com.zatiaras.pos.feature.pos.navigation.posScreen
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptEvent
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptScreen
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptViewModel
import com.zatiaras.pos.feature.reports.navigation.navigateToPnlReport
import com.zatiaras.pos.feature.reports.navigation.navigateToReports
import com.zatiaras.pos.feature.reports.navigation.pnlReportScreen
import com.zatiaras.pos.feature.reports.navigation.reportsScreen
import com.zatiaras.pos.feature.printer.navigation.navigateToPrinterSettings
import com.zatiaras.pos.feature.printer.navigation.printerSettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var cartHolder: CartHolder
    
    @Inject
    lateinit var transactionHolder: TransactionHolder
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZatiarasPOSTheme {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.LOGIN,
                        modifier = Modifier.padding(innerPadding)
                    ) {
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
                            onLogout = {
                                navController.navigate(NavRoutes.LOGIN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                        
                        // Reports P&L (Full Screen - if accessed from outside tabs, but typically accessed from dashboard tab)
                        // In MainScreen logic, we passed 'onNavigateToPnl' to the dashboard.
                        // If P&L should be full screen (covering bottom bar), keep it here.
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
                                            Toast.makeText(
                                                this@MainActivity,
                                                event.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
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
                                        // Go back to POS and clear the backstack
                                        navController.navigate(PosRoutes.POS) {
                                            popUpTo(NavRoutes.HOME) { inclusive = false }
                                        }
                                    },
                                    onPrintReceipt = {
                                        if (receiptUiState.isPrinterConnected) {
                                            receiptViewModel.printReceipt()
                                        } else {
                                            // Navigate to printer settings if not connected
                                            navController.navigateToPrinterSettings()
                                        }
                                    },
                                    isPrinting = receiptUiState.isPrinting,
                                    isPrinterConnected = receiptUiState.isPrinterConnected,
                                    printerName = receiptUiState.printerName
                                )
                            } else {
                                // Loading or fallback - should rarely happen
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Navigation route constants for the app.
 * Type-safe navigation will be implemented when more routes are added.
 */
object NavRoutes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val POS = "pos"
    const val CHECKOUT = "checkout"
    const val RECEIPT = "receipt"
    const val INVENTORY = "inventory"
    const val TRANSACTIONS = "transactions"
    const val CASH_RECORD = "cash_record"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
}


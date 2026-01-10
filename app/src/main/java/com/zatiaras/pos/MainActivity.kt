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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zatiaras.pos.core.ui.theme.ZatiarasPOSTheme
import com.zatiaras.pos.feature.auth.LoginRoute
import com.zatiaras.pos.feature.auth.navigation.AuthRoutes
import com.zatiaras.pos.feature.auth.navigation.pinSetupScreen
import com.zatiaras.pos.feature.auth.navigation.settingsScreen
import com.zatiaras.pos.feature.home.HomeRoute
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
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptScreen
import com.zatiaras.pos.feature.reports.navigation.navigateToReports
import com.zatiaras.pos.feature.reports.navigation.reportsScreen
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
                            HomeRoute(
                                onNavigateToPOS = {
                                    navController.navigateToPos()
                                },
                                onNavigateToInventory = {
                                    navController.navigate(InventoryNavigation.INVENTORY_ROUTE)
                                },
                                onNavigateToTransactions = {
                                    navController.navigateToCashRecord()
                                },
                                onNavigateToReports = {
                                    navController.navigateToReports()
                                },
                                onNavigateToSettings = {
                                    navController.navigate(AuthRoutes.SETTINGS)
                                },
                                onLogout = {
                                    navController.navigate(NavRoutes.LOGIN) {
                                        popUpTo(NavRoutes.HOME) { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // Inventory feature navigation graph
                        inventoryNavGraph(navController)
                        
                        // POS feature screens
                        posScreen(
                            cartHolder = cartHolder,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToCheckout = {
                                navController.navigateToCheckout()
                            }
                        )
                        
                        checkoutScreen(
                            cartHolder = cartHolder,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onTransactionComplete = { transaction ->
                                // Store transaction for receipt screen
                                transactionHolder.setTransaction(transaction)
                                // Navigate to receipt screen
                                navController.navigate(NavRoutes.RECEIPT) {
                                    // Clear checkout from backstack
                                    popUpTo(PosRoutes.POS) { inclusive = false }
                                }
                            }
                        )
                        
                        // Cash Record (Buku Kas) screen
                        cashRecordScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                        
                        // Settings screen
                        settingsScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToPinSetup = {
                                navController.navigate(AuthRoutes.PIN_SETUP)
                            },
                            onLogout = {
                                navController.navigate(NavRoutes.LOGIN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                        
                        // PIN Setup screen
                        // Reports Dashboard
                        reportsScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                        
                        pinSetupScreen(
                            onPinSet = {
                                navController.popBackStack()
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                        
                        // Receipt screen
                        composable(NavRoutes.RECEIPT) {
                            var transaction by remember { mutableStateOf<Transaction?>(null) }
                            
                            LaunchedEffect(Unit) {
                                transaction = transactionHolder.consumeTransaction()
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
                                        // TODO: Implement print in Phase 7
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Fitur cetak akan tersedia segera",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
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


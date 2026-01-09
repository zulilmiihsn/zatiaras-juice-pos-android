package com.zatiaras.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zatiaras.pos.core.ui.theme.ZatiarasPOSTheme
import com.zatiaras.pos.feature.auth.LoginRoute
import com.zatiaras.pos.feature.home.HomeRoute
import com.zatiaras.pos.feature.inventory.navigation.InventoryNavigation
import com.zatiaras.pos.feature.inventory.navigation.inventoryNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
                                    // TODO: Navigate to POS when implemented
                                },
                                onNavigateToInventory = {
                                    navController.navigate(InventoryNavigation.INVENTORY_ROUTE)
                                },
                                onNavigateToTransactions = {
                                    // TODO: Navigate to Transactions when implemented
                                },
                                onNavigateToReports = {
                                    // TODO: Navigate to Reports when implemented
                                },
                                onNavigateToSettings = {
                                    // TODO: Navigate to Settings when implemented
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
    const val INVENTORY = "inventory"
    const val TRANSACTIONS = "transactions"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
}


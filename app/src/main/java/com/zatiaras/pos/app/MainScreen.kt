package com.zatiaras.pos.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zatiaras.pos.NavRoutes
import com.zatiaras.pos.feature.pos.domain.model.CartHolder
import com.zatiaras.pos.feature.pos.navigation.PosRoutes
import com.zatiaras.pos.feature.pos.navigation.cashRecordScreen
import com.zatiaras.pos.feature.pos.navigation.posScreen
import com.zatiaras.pos.feature.reports.navigation.homeDashboardScreen
import com.zatiaras.pos.feature.reports.navigation.reportsScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(NavRoutes.HOME, "Beranda", Icons.Default.Home)
    data object Pos : BottomNavItem(PosRoutes.POS, "Kasir", Icons.Default.ShoppingCart)
    data object CashRecord : BottomNavItem(NavRoutes.CASH_RECORD, "Catat", Icons.Default.Receipt)
    data object Reports : BottomNavItem(NavRoutes.REPORTS, "Laporan", Icons.Default.Analytics)
}

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    cartHolder: CartHolder,
    onNavigateBackFromMain: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    onNavigateToPnl: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Pos,
        BottomNavItem.CashRecord,
        BottomNavItem.Reports
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Only show bottom bar on top-level destinations
            val isTopLevel = items.any { it.route == currentDestination?.route }
            
            if (isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp // Flat styling (ShadCN)
                ) {
                    items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Tab 1: Home (Quick Overview Dashboard)
            homeDashboardScreen(
                route = NavRoutes.HOME,
                onNavigateToSettings = onNavigateToSettings
            )
            
            // Tab 2: POS
            posScreen(
                cartHolder = cartHolder,
                onNavigateBack = { /* No back action for tab */ },
                onNavigateToCheckout = onNavigateToCheckout
            )
            
            // Tab 3: Cash Record (Buku Kas)
            cashRecordScreen(
                onNavigateBack = { /* No back action for tab */ }
            )
            
            // Tab 4: Reports (Detailed Reports with Charts & P&L)
            reportsScreen(
                route = NavRoutes.REPORTS,
                onNavigateBack = null, // No back button for tab
                onNavigateToPnl = onNavigateToPnl
            )
        }
    }
}


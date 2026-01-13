package com.zatiaras.pos.feature.inventory.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zatiaras.pos.feature.inventory.presentation.detail.ProductDetailScreen
import com.zatiaras.pos.feature.inventory.presentation.list.InventoryScreen

/**
 * Navigation routes for Inventory feature.
 */
object InventoryNavigation {
    const val INVENTORY_ROUTE = "inventory"
    const val PRODUCT_DETAIL_ROUTE = "inventory/product/{productId}"
    const val PRODUCT_CREATE_ROUTE = "inventory/product/new"
    
    fun productDetailRoute(productId: String) = "inventory/product/$productId"
}

/**
 * Extension function to add Inventory navigation graph.
 * 
 * Usage in MainActivity:
 * ```
 * NavHost(...) {
 *     inventoryNavGraph(navController)
 * }
 * ```
 */
/**
 * Extension function to add Inventory navigation graph.
 * 
 * Usage in MainActivity:
 * ```
 * NavHost(...) {
 *     inventoryNavGraph(navController, accessControlManager)
 * }
 * ```
 */
fun NavGraphBuilder.inventoryNavGraph(
    navController: NavController,
    accessControlManager: com.zatiaras.pos.core.data.access.AccessControlManager? = null
) {
    // Inventory List Screen
    composable(InventoryNavigation.INVENTORY_ROUTE) {
        if (accessControlManager != null) {
            com.zatiaras.pos.core.ui.components.AccessControlGate(
                accessControlManager = accessControlManager,
                route = com.zatiaras.pos.core.data.access.LockableRoute.INVENTORY.route,
                screenName = "Inventaris",
                onAccessDenied = { navController.popBackStack() }
            ) {
                InventoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { productId ->
                        if (productId != null) {
                            navController.navigate(InventoryNavigation.productDetailRoute(productId))
                        } else {
                            navController.navigate(InventoryNavigation.PRODUCT_CREATE_ROUTE)
                        }
                    }
                )
            }
        } else {
            InventoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { productId ->
                    if (productId != null) {
                        navController.navigate(InventoryNavigation.productDetailRoute(productId))
                    } else {
                        navController.navigate(InventoryNavigation.PRODUCT_CREATE_ROUTE)
                    }
                }
            )
        }
    }
    
    // Product Detail Screen (Edit mode)
    composable(
        route = InventoryNavigation.PRODUCT_DETAIL_ROUTE,
        arguments = listOf(
            navArgument("productId") { type = NavType.StringType }
        )
    ) {
        ProductDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onSaveSuccess = { navController.popBackStack() }
        )
    }
    
    // Product Create Screen (Create mode)
    composable(InventoryNavigation.PRODUCT_CREATE_ROUTE) {
        ProductDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onSaveSuccess = { navController.popBackStack() }
        )
    }
}

/**
 * Navigate to Inventory list screen.
 */
fun NavController.navigateToInventory() {
    navigate(InventoryNavigation.INVENTORY_ROUTE)
}

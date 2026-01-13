package com.zatiaras.pos.feature.pos.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.zatiaras.pos.feature.pos.domain.model.CartHolder
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.presentation.PosScreen
import com.zatiaras.pos.feature.pos.presentation.PosViewModel
import com.zatiaras.pos.feature.pos.presentation.cashrecord.CashRecordScreen
import com.zatiaras.pos.feature.pos.presentation.checkout.CheckoutScreen
import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptScreen

/**
 * Route constants for POS feature screens.
 */
object PosRoutes {
    const val POS = "pos"
    const val CHECKOUT = "checkout"
    const val RECEIPT = "receipt/{transactionId}"
    const val CASH_RECORD = "cash_record"
    
    fun receipt(transactionId: String) = "receipt/$transactionId"
}

// Keep backward compatibility
const val POS_ROUTE = PosRoutes.POS
const val CHECKOUT_ROUTE = PosRoutes.CHECKOUT
const val CASH_RECORD_ROUTE = PosRoutes.CASH_RECORD

/**
 * Navigate to POS screen.
 */
fun NavController.navigateToPos(navOptions: NavOptions? = null) {
    navigate(PosRoutes.POS, navOptions)
}

/**
 * Navigate to Checkout screen.
 */
fun NavController.navigateToCheckout(navOptions: NavOptions? = null) {
    navigate(PosRoutes.CHECKOUT, navOptions)
}

/**
 * Navigate to Receipt screen.
 */
fun NavController.navigateToReceipt(transactionId: String, navOptions: NavOptions? = null) {
    navigate(PosRoutes.receipt(transactionId), navOptions)
}

/**
 * Navigate to Cash Record (Buku Kas) screen.
 */
fun NavController.navigateToCashRecord(navOptions: NavOptions? = null) {
    navigate(PosRoutes.CASH_RECORD, navOptions)
}

/**
 * Add POS catalog screen to NavGraph.
 */
fun NavGraphBuilder.posScreen(
    cartHolder: CartHolder,
    onNavigateBack: () -> Unit,
    onNavigateToCheckout: () -> Unit
) {
    composable(route = PosRoutes.POS) {
        val viewModel: PosViewModel = hiltViewModel()
        
        PosScreen(
            onNavigateBack = onNavigateBack,
            onProceedToCheckout = {
                // Save cart to holder before navigating
                cartHolder.updateCart(viewModel.getCurrentCart())
                onNavigateToCheckout()
            },
            viewModel = viewModel
        )
    }
}

/**
 * Add Checkout screen to NavGraph.
 */
fun NavGraphBuilder.checkoutScreen(
    cartHolder: CartHolder,
    onNavigateBack: () -> Unit,
    onTransactionComplete: (Transaction) -> Unit
) {
    composable(route = PosRoutes.CHECKOUT) {
        val cart by cartHolder.cart.collectAsState()
        
        CheckoutScreen(
            cart = cart,
            onNavigateBack = onNavigateBack,
            onTransactionComplete = { transaction ->
                // Clear cart after successful transaction
                cartHolder.clearCart()
                onTransactionComplete(transaction)
            }
        )
    }
}

/**
 * Add Cash Record (Buku Kas) screen to NavGraph.
 * Protected by Access Control if manager is provided.
 */
fun NavGraphBuilder.cashRecordScreen(
    onNavigateBack: () -> Unit,
    accessControlManager: com.zatiaras.pos.core.data.access.AccessControlManager? = null
) {
    composable(route = PosRoutes.CASH_RECORD) {
        if (accessControlManager != null) {
            com.zatiaras.pos.core.ui.components.AccessControlGate(
                accessControlManager = accessControlManager,
                route = com.zatiaras.pos.core.data.access.LockableRoute.CASH_RECORD_TAB.route,
                screenName = "Buku Kas",
                onAccessDenied = onNavigateBack
            ) {
                CashRecordScreen(
                    onNavigateBack = onNavigateBack
                )
            }
        } else {
            CashRecordScreen(
                onNavigateBack = onNavigateBack
            )
        }
    }
}

/**
 * Add Receipt screen to NavGraph.
 */
fun NavGraphBuilder.receiptScreen(
    onNewTransaction: () -> Unit,
    onPrintReceipt: (Transaction) -> Unit,
    getTransaction: suspend (String) -> Transaction?
) {
    composable(route = PosRoutes.RECEIPT) { backStackEntry ->
        val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
        
        // For now, we'll pass transaction via a different mechanism
        // This is a placeholder - in real app, fetch from repository
        // TODO: Implement proper transaction retrieval
    }
}

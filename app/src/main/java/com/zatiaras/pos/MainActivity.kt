package com.zatiaras.pos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.zatiaras.pos.core.data.access.AccessControlManager
import com.zatiaras.pos.core.ui.theme.ZatiarasPOSTheme
import com.zatiaras.pos.feature.pos.domain.model.CartHolder
import com.zatiaras.pos.feature.pos.domain.model.TransactionHolder
import com.zatiaras.pos.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point for the ZatiarasPOS application.
 * 
 * Uses FragmentActivity to support biometric authentication prompts.
 * Simplified to follow KISS principle - navigation logic extracted to AppNavGraph.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var cartHolder: CartHolder
    
    @Inject
    lateinit var transactionHolder: TransactionHolder
    
    @Inject
    lateinit var accessControlManager: AccessControlManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZatiarasPOSTheme {
                val navController = rememberNavController()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        cartHolder = cartHolder,
                        transactionHolder = transactionHolder,
                        accessControlManager = accessControlManager,
                        modifier = Modifier.padding(innerPadding)
                    )
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
    const val STARTUP = "startup"
    const val LOGIN = "login"
    const val APP_LOCK = "app_lock"
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

package com.zatiaras.pos.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zatiaras.pos.core.data.access.AccessCheckResult
import com.zatiaras.pos.core.data.access.AccessControlManager
import kotlinx.coroutines.launch

/**
 * State for access control gate.
 */
sealed class AccessGateState {
    data object Loading : AccessGateState()
    data object Granted : AccessGateState()
    data object RequiresPin : AccessGateState()
    data object Denied : AccessGateState()
}

/**
 * A composable wrapper that protects content based on access control rules.
 * 
 * Usage:
 * ```
 * AccessControlGate(
 *     accessControlManager = accessControlManager,
 *     route = LockableRoute.SETTINGS.route,
 *     screenName = "Pengaturan",
 *     onAccessDenied = { navController.popBackStack() }
 * ) {
 *     SettingsScreen(...)
 * }
 * ```
 * 
 * Behavior:
 * - If user is Owner: Shows content immediately
 * - If route is not locked: Shows content immediately
 * - If route is locked for Kasir: Shows PIN dialog first
 * - On successful PIN entry: Shows content
 * - On dismiss: Calls onAccessDenied (usually navigate back)
 * 
 * @param accessControlManager The access control manager instance
 * @param route The route to check access for
 * @param screenName Display name for the screen (shown in PIN dialog)
 * @param onAccessDenied Called when user cancels PIN entry
 * @param content The protected content to show when access is granted
 */
@Composable
fun AccessControlGate(
    accessControlManager: AccessControlManager,
    route: String,
    screenName: String,
    onAccessDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    var gateState by remember { mutableStateOf<AccessGateState>(AccessGateState.Loading) }
    var showPinDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Check access on first composition
    LaunchedEffect(route) {
        val result = accessControlManager.checkAccessNow(route)
        gateState = when (result) {
            is AccessCheckResult.Granted,
            is AccessCheckResult.GrantedNoPinSet -> AccessGateState.Granted
            is AccessCheckResult.RequiresOwnerPin -> AccessGateState.RequiresPin
        }
        
        if (gateState == AccessGateState.RequiresPin) {
            showPinDialog = true
        }
    }

    when (gateState) {
        AccessGateState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        AccessGateState.Granted -> {
            content()
        }

        AccessGateState.RequiresPin -> {
            // Show a placeholder while waiting for PIN
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            if (showPinDialog) {
                OwnerPinDialog(
                    onDismiss = {
                        showPinDialog = false
                        onAccessDenied()
                    },
                    onPinVerified = {
                        showPinDialog = false
                        gateState = AccessGateState.Granted
                    },
                    verifyPin = { pin ->
                        accessControlManager.verifyOwnerPin(pin)
                    },
                    screenName = screenName
                )
            }
        }

        AccessGateState.Denied -> {
            // This state shouldn't normally occur, but handle it gracefully
            LaunchedEffect(Unit) {
                onAccessDenied()
            }
        }
    }
}

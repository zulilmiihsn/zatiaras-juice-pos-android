package com.zatiaras.pos.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A reusable dialog wrapper that provides a uniform "slide up" animation
 * for all modal dialogs in the Zatiaras application.
 */
@Composable
fun ZatDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable (dismiss: () -> Unit) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Trigger enter animation
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Function to handle dismissal with animation
    val animateDismiss = {
        if (!isDismissing) {
            isDismissing = true
            scope.launch {
                isVisible = false
                delay(280) // Animation duration is 300ms
                onDismissRequest()
            }
        }
    }

    Dialog(
        onDismissRequest = { animateDismiss() },
        properties = properties
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                content(animateDismiss)
            }
        }
    }
}

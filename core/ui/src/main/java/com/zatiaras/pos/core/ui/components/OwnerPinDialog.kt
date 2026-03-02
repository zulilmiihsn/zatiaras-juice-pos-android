package com.zatiaras.pos.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.core.ui.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zatiaras.pos.core.ui.theme.AppShapes
import kotlinx.coroutines.delay

/**
 * Dialog for entering owner PIN to access locked screens.
 * 
 * Shows a 4-digit PIN keypad with:
 * - Lock icon and title
 * - PIN dots indicator
 * - Numeric keypad
 * - Error state with shake animation
 * 
 * @param onDismiss Called when dialog is dismissed
 * @param onPinVerified Called when PIN is successfully verified
 * @param verifyPin Suspend function to verify PIN against stored hash
 * @param screenName Name of the screen being accessed (for display)
 */
@Composable
fun OwnerPinDialog(
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit,
    verifyPin: suspend (String) -> Boolean,
    screenName: String = stringResource(R.string.core_halaman_ini)
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    
    ZatDialog(
        onDismissRequest = onDismiss
    ) { dismiss ->
        // Auto-verify when 4 digits entered
        LaunchedEffect(pin) {
            if (pin.length == 4) {
                isVerifying = true
                val isValid = verifyPin(pin)
                isVerifying = false
                
                if (isValid) {
                    onPinVerified()
                    dismiss()
                } else {
                    isError = true
                    delay(500)
                    isError = false
                    pin = ""
                }
            }
        }
        
        Box(
            modifier = Modifier.fillMaxWidth(0.95f),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.XXL),
                shape = AppShapes.XXL,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Lock Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.core_menu_terkunci),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.core_masukkan_pin_akses, screenName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // PIN Dots
                    PinDotsIndicator(
                        pinLength = pin.length,
                        isError = isError
                    )

                    if (isError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.core_pin_salah),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Numeric Keypad
                    PinKeypad(
                        onDigitClick = { digit ->
                            if (pin.length < 4 && !isVerifying) {
                                pin += digit
                            }
                        },
                        onDeleteClick = {
                            if (pin.isNotEmpty() && !isVerifying) {
                                pin = pin.dropLast(1)
                            }
                        },
                        enabled = !isVerifying
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Cancel Button
                    TextButton(
                        onClick = dismiss,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text(stringResource(R.string.core_batal))
                    }
                }
            }
        }
    }
}

/**
 * PIN dots indicator showing entered digits.
 */
@Composable
private fun PinDotsIndicator(
    pinLength: Int,
    isError: Boolean
) {
    val maxLength = 4
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLength) { index ->
            val isFilled = index < pinLength
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> MaterialTheme.colorScheme.error
                            isFilled -> MaterialTheme.colorScheme.primary
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        width = 2.dp,
                        color = when {
                            isError -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Numeric keypad for PIN entry.
 */
@Composable
private fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    enabled: Boolean
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "del")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> {
                            Spacer(modifier = Modifier.size(64.dp))
                        }
                        "del" -> {
                            KeypadButton(
                                onClick = onDeleteClick,
                                enabled = enabled
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            KeypadButton(
                                onClick = { onDigitClick(key) },
                                enabled = enabled
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual keypad button.
 */
@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

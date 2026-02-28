package com.zatiaras.pos.feature.auth.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.auth.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.theme.LocalDimensions

@Composable
fun AppLockRoute(
    onUnlocked: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Safe cast to FragmentActivity for biometric support
    val fragmentActivity = context as? FragmentActivity

    LaunchedEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) {
            onUnlocked()
        }
    }

    // Auto-trigger biometric on first load if enabled
    // Use the state values as key so it triggers after settings are loaded
    LaunchedEffect(uiState.biometricEnabled, uiState.biometricAvailable) {
        if (uiState.biometricEnabled && uiState.biometricAvailable && fragmentActivity != null) {
            viewModel.authenticateWithBiometric(fragmentActivity)
        }
    }

    AppLockScreen(
        uiState = uiState,
        onPinDigitClick = viewModel::onPinDigitClick,
        onBackspaceClick = viewModel::onBackspaceClick,
        onBiometricClick = { 
            fragmentActivity?.let { viewModel.authenticateWithBiometric(it) }
        }
    )
}

@Composable
fun AppLockScreen(
    uiState: AppLockUiState,
    onPinDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        val dimensions = LocalDimensions.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensions.paddingXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(dimensions.iconSizeXL))

            // Lock Icon and Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(dimensions.spacingL))

                Text(
                    text = stringResource(R.string.auth_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(dimensions.spacingXS))

                Text(
                    text = if (uiState.showPinInput) stringResource(R.string.app_lock_enter_pin) else stringResource(R.string.app_lock_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // PIN Display
            if (uiState.showPinInput || uiState.pinSet) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // PIN Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM),
                        modifier = Modifier.padding(vertical = dimensions.paddingXXL)
                    ) {
                        repeat(4) { index ->
                            PinDot(
                                filled = index < uiState.enteredPin.length,
                                error = uiState.pinError
                            )
                        }
                    }

                    // Error Message
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // PIN Keypad
                    PinKeypad(
                        onDigitClick = onPinDigitClick,
                        onBackspaceClick = onBackspaceClick,
                        onBiometricClick = if (uiState.biometricEnabled && uiState.biometricAvailable) {
                            onBiometricClick
                        } else null
                    )
                }
            } else {
                // Biometric Only Mode
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 32.dp)
                ) {
                    if (uiState.biometricAvailable) {
                        FilledTonalButton(
                            onClick = onBiometricClick,
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = stringResource(R.string.app_lock_biometric),
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(dimensions.spacingM))

                        Text(
                            text = stringResource(R.string.app_lock_touch),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensions.iconSizeXL))
        }
    }
}

@Composable
private fun PinDot(
    filled: Boolean,
    error: Boolean
) {
    val color = when {
        error -> MaterialTheme.colorScheme.error
        filled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .then(
                if (filled) {
                    Modifier.background(color)
                } else {
                    Modifier.border(2.dp, color, CircleShape)
                }
            )
    )
}

@Composable
private fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: (() -> Unit)?
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("bio", "0", "del")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        digits.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        "bio" -> {
                            if (onBiometricClick != null) {
                                KeypadButton(
                                    onClick = onBiometricClick,
                                    content = {
                                        Icon(
                                            imageVector = Icons.Filled.Fingerprint,
                                            contentDescription = stringResource(R.string.app_lock_biometric),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(72.dp))
                            }
                        }
                        "del" -> {
                            KeypadButton(
                                onClick = onBackspaceClick,
                                content = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = stringResource(R.string.auth_delete),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            )
                        }
                        else -> {
                            KeypadButton(
                                onClick = { onDigitClick(key) },
                                content = {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            content()
        }
    }
}

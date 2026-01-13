package com.zatiaras.pos.feature.auth.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.theme.LocalDimensions

enum class PinSetupStep {
    ENTER_NEW_PIN,
    CONFIRM_PIN,
    VERIFY_CURRENT_PIN // For changing existing PIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupRoute(
    isChangingPin: Boolean = false,
    onPinSet: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PinSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isPinSet) {
        if (uiState.isPinSet) {
            onPinSet()
        }
    }

    LaunchedEffect(isChangingPin) {
        viewModel.setIsChangingPin(isChangingPin)
    }

    PinSetupScreen(
        uiState = uiState,
        onDigitClick = viewModel::onDigitClick,
        onBackspaceClick = viewModel::onBackspaceClick,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    uiState: PinSetupUiState,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        when (uiState.step) {
                            PinSetupStep.VERIFY_CURRENT_PIN -> "Verifikasi PIN"
                            PinSetupStep.ENTER_NEW_PIN -> "Atur PIN Baru"
                            PinSetupStep.CONFIRM_PIN -> "Konfirmasi PIN"
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { padding ->
        val dimensions = LocalDimensions.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(dimensions.paddingXL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(dimensions.paddingXXL))

            // Instruction Text
            Text(
                text = when (uiState.step) {
                    PinSetupStep.VERIFY_CURRENT_PIN -> "Masukkan PIN Anda saat ini"
                    PinSetupStep.ENTER_NEW_PIN -> "Masukkan PIN baru (4 digit)"
                    PinSetupStep.CONFIRM_PIN -> "Masukkan ulang PIN untuk konfirmasi"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(dimensions.paddingXXL))

            // PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM),
                modifier = Modifier.padding(vertical = dimensions.spacingM)
            ) {
                repeat(4) { index ->
                    PinSetupDot(
                        filled = index < uiState.currentPin.length,
                        error = uiState.hasError
                    )
                }
            }

            // Error or Success Message
            Box(
                modifier = Modifier.height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.errorMessage != null -> {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    uiState.isPinSet -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "PIN berhasil diatur!",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // PIN Keypad
            PinSetupKeypad(
                onDigitClick = onDigitClick,
                onBackspaceClick = onBackspaceClick
            )

            Spacer(modifier = Modifier.height(dimensions.paddingXXL))
        }
    }
}

@Composable
private fun PinSetupDot(
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
            .size(20.dp)
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
private fun PinSetupKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "del")
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
                        "" -> {
                            Spacer(modifier = Modifier.size(72.dp))
                        }
                        "del" -> {
                            KeypadSetupButton(
                                onClick = onBackspaceClick,
                                content = {
                                    Icon(
                                        imageVector = Icons.Filled.Backspace,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            )
                        }
                        else -> {
                            KeypadSetupButton(
                                onClick = { onDigitClick(key) },
                                content = {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
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
private fun KeypadSetupButton(
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

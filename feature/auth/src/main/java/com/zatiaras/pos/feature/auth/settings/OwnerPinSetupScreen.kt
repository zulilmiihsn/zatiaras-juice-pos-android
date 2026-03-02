package com.zatiaras.pos.feature.auth.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.auth.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OwnerPinSetupStep {
    ENTER_NEW_PIN,
    CONFIRM_PIN
}

/**
 * Owner PIN Setup Screen with tap keypad (matching Security PIN style)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerPinSetupScreen(
    onPinSet: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimensions = LocalDimensions.current
    
    var step by remember { mutableStateOf(OwnerPinSetupStep.ENTER_NEW_PIN) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinSet by remember { mutableStateOf(false) }
    val pinMismatchMessage = stringResource(R.string.pin_setup_mismatch)
    
    // Handle PIN set success
    LaunchedEffect(isPinSet) {
        if (isPinSet) {
            delay(500) // Show success message briefly
            onPinSet()
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        when (step) {
                            OwnerPinSetupStep.ENTER_NEW_PIN -> if (uiState.ownerPinSet) stringResource(R.string.access_control_change_pin) else stringResource(R.string.access_control_set_new)
                            OwnerPinSetupStep.CONFIRM_PIN -> stringResource(R.string.pin_setup_confirm)
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.auth_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        val scope = rememberCoroutineScope()
        
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
                text = when (step) {
                    OwnerPinSetupStep.ENTER_NEW_PIN -> stringResource(R.string.pin_setup_enter_new_4digit)
                    OwnerPinSetupStep.CONFIRM_PIN -> stringResource(R.string.pin_setup_reenter_confirm)
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
                    OwnerPinDot(
                        filled = index < currentPin.length,
                        error = hasError
                    )
                }
            }

            // Error or Success Message
            Box(
                modifier = Modifier.height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    isPinSet -> {
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
                                text = stringResource(R.string.owner_pin_set_success),
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
            OwnerPinKeypad(
                onDigitClick = { digit ->
                    if (currentPin.length >= 4) return@OwnerPinKeypad
                    
                    val newPinInput = currentPin + digit
                    currentPin = newPinInput
                    hasError = false
                    errorMessage = null
                    
                    // Auto-process when 4 digits entered
                    if (newPinInput.length == 4) {
                        scope.launch {
                            when (step) {
                                OwnerPinSetupStep.ENTER_NEW_PIN -> {
                                    // Add delay so user can see 4th dot filled
                                    delay(200)
                                    newPin = newPinInput
                                    currentPin = ""
                                    step = OwnerPinSetupStep.CONFIRM_PIN
                                }
                                OwnerPinSetupStep.CONFIRM_PIN -> {
                                    // Add delay so user can see 4th dot filled
                                    delay(200)
                                    if (newPinInput == newPin) {
                                        // PINs match, save it
                                        viewModel.setOwnerPin(newPinInput)
                                        isPinSet = true
                                    } else {
                                        // PINs don't match
                                        hasError = true
                                        errorMessage = pinMismatchMessage
                                        currentPin = ""
                                        // Reset after delay
                                        delay(1500)
                                        step = OwnerPinSetupStep.ENTER_NEW_PIN
                                        newPin = ""
                                        hasError = false
                                        errorMessage = null
                                    }
                                }
                            }
                        }
                    }
                },
                onBackspaceClick = {
                    if (currentPin.isNotEmpty()) {
                        currentPin = currentPin.dropLast(1)
                        hasError = false
                        errorMessage = null
                    }
                }
            )

            Spacer(modifier = Modifier.height(dimensions.paddingXXL))
        }
    }
}

@Composable
private fun OwnerPinDot(
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
private fun OwnerPinKeypad(
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
                            OwnerKeypadButton(
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
                            OwnerKeypadButton(
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
private fun OwnerKeypadButton(
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

package com.zatiaras.pos.feature.auth.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.auth.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.InfoBlue
import com.zatiaras.pos.core.ui.theme.PurpleAccent
import com.zatiaras.pos.core.ui.theme.SuccessGreen
import com.zatiaras.pos.core.ui.theme.WarningAmber
import androidx.compose.foundation.border

// Icon colors for consistent theming
private val LockIconColor = Brand500
private val BiometricIconColor = PurpleAccent
private val PinIconColor = InfoBlue
private val PasswordIconColor = WarningAmber
private val SuccessColor = SuccessGreen
private val InfoColor = InfoBlue

/**
 * Security Settings Sub-Screen - Enhanced with premium styling
 * Contains: App Lock, Biometric, PIN settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onNavigateBack: () -> Unit,
    onChangePinClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // Refresh PIN status when screen becomes visible (e.g., after returning from PIN setup)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPinStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.sec_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.auth_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header with security status
            SecurityStatusHeader(
                isLockEnabled = uiState.lockEnabled,
                isPinSet = uiState.pinSet,
                isBiometricEnabled = uiState.biometricEnabled
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section Header
            SectionHeader(
                title = stringResource(R.string.sec_app_lock),
                subtitle = stringResource(R.string.sec_app_lock_desc)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Security Card - Enhanced
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.XL),
                shape = AppShapes.XL,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    // App Lock Toggle
                    EnhancedSecurityItem(
                        icon = Icons.Outlined.Lock,
                        iconColor = LockIconColor,
                        title = stringResource(R.string.sec_app_lock_switch),
                        subtitle = if (uiState.lockEnabled) stringResource(R.string.sec_active_app_lock) else stringResource(R.string.sec_app_lock_switch_desc),
                        isEnabled = uiState.lockEnabled,
                        trailing = {
                            Switch(
                                checked = uiState.lockEnabled,
                                onCheckedChange = { viewModel.setLockEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                                    checkedTrackColor = LockIconColor
                                )
                            )
                        }
                    )

                    if (uiState.lockEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Biometric Toggle
                        EnhancedSecurityItem(
                            icon = Icons.Outlined.Fingerprint,
                            iconColor = BiometricIconColor,
                            title = stringResource(R.string.sec_biometric),
                            subtitle = when {
                                !uiState.biometricAvailable -> stringResource(R.string.sec_bio_not_avail)
                                uiState.biometricEnabled -> stringResource(R.string.sec_bio_active)
                                else -> stringResource(R.string.sec_bio_desc)
                            },
                            isEnabled = uiState.biometricEnabled,
                            trailing = {
                                Switch(
                                    checked = uiState.biometricEnabled,
                                    onCheckedChange = { viewModel.setBiometricEnabled(it) },
                                    enabled = uiState.biometricAvailable,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                                        checkedTrackColor = BiometricIconColor
                                    )
                                )
                            }
                        )

                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SecuritySubsectionLabel(
                        title = stringResource(R.string.sec_pin_module_title),
                        subtitle = stringResource(R.string.sec_pin_module_desc)
                    )

                    // PIN module (independent from password module)
                    EnhancedSecurityItem(
                        icon = Icons.Outlined.Pin,
                        iconColor = PinIconColor,
                        title = if (uiState.pinSet) stringResource(R.string.sec_change_pin) else stringResource(R.string.sec_set_pin),
                        subtitle = if (uiState.pinSet) stringResource(R.string.sec_pin_set) else stringResource(R.string.sec_pin_not_set),
                        isEnabled = uiState.pinSet,
                        trailing = {
                            Button(
                                onClick = onChangePinClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PinIconColor
                                ),
                                shape = AppShapes.M,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    if (uiState.pinSet) stringResource(R.string.auth_change) else stringResource(R.string.auth_configure),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SecuritySubsectionLabel(
                        title = stringResource(R.string.sec_password_module_title),
                        subtitle = stringResource(R.string.sec_password_module_desc)
                    )

                    // Password module (independent from PIN module)
                    EnhancedSecurityItem(
                        icon = Icons.Outlined.Password,
                        iconColor = PasswordIconColor,
                        title = stringResource(R.string.sec_change_password),
                        subtitle = stringResource(R.string.sec_change_password_desc),
                        isEnabled = true,
                        trailing = {
                            Button(
                                onClick = {
                                    viewModel.clearPasswordChangeMessage()
                                    showChangePasswordDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PasswordIconColor
                                ),
                                shape = AppShapes.M,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    stringResource(R.string.auth_change),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Box - Enhanced
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.L,
                colors = CardDefaults.cardColors(
                    containerColor = InfoColor.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(AppShapes.M)
                            .background(InfoColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = InfoColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.sec_tips_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InfoColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.sec_tips_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                isLoading = uiState.isChangingPassword,
                errorMessageType = uiState.passwordChangeError,
                errorMessageDetail = uiState.passwordChangeErrorDetail,
                successMessageType = uiState.passwordChangeSuccess,
                onDismiss = {
                    if (!uiState.isChangingPassword) {
                        showChangePasswordDialog = false
                        viewModel.clearPasswordChangeMessage()
                    }
                },
                onSubmit = { currentPassword, newPassword, confirmPassword ->
                    viewModel.changePassword(currentPassword, newPassword, confirmPassword)
                },
                onSuccessAcknowledge = {
                    showChangePasswordDialog = false
                    viewModel.clearPasswordChangeMessage()
                }
            )
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    errorMessageType: PasswordChangeMessage?,
    errorMessageDetail: String?,
    successMessageType: PasswordChangeMessage?,
    onDismiss: () -> Unit,
    onSubmit: (currentPassword: String, newPassword: String, confirmPassword: String) -> Unit,
    onSuccessAcknowledge: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val successMessage = when (successMessageType) {
        PasswordChangeMessage.PASSWORD_CHANGED -> stringResource(R.string.sec_password_changed_success)
        else -> null
    }

    val errorMessage = when (errorMessageType) {
        PasswordChangeMessage.REQUIRED_FIELDS -> stringResource(R.string.sec_error_required_fields)
        PasswordChangeMessage.MIN_LENGTH -> stringResource(R.string.sec_error_min_length)
        PasswordChangeMessage.CONFIRMATION_MISMATCH -> stringResource(R.string.sec_error_confirmation_mismatch)
        PasswordChangeMessage.SAME_AS_CURRENT -> stringResource(R.string.sec_error_same_as_current)
        PasswordChangeMessage.GENERIC_FAILURE -> errorMessageDetail ?: stringResource(R.string.sec_error_change_failed)
        else -> null
    }

    if (successMessage != null) {
        AlertDialog(
            onDismissRequest = onSuccessAcknowledge,
            title = { Text(text = stringResource(R.string.sec_change_password)) },
            text = { Text(text = successMessage) },
            confirmButton = {
                TextButton(onClick = onSuccessAcknowledge) {
                    Text(text = stringResource(R.string.auth_back))
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.sec_change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    singleLine = true,
                    enabled = !isLoading,
                    label = { Text(stringResource(R.string.sec_current_password)) },
                    visualTransformation = if (currentVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentVisible = !currentVisible }) {
                            Icon(
                                imageVector = if (currentVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    singleLine = true,
                    enabled = !isLoading,
                    label = { Text(stringResource(R.string.sec_new_password)) },
                    visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newVisible = !newVisible }) {
                            Icon(
                                imageVector = if (newVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    singleLine = true,
                    enabled = !isLoading,
                    label = { Text(stringResource(R.string.sec_confirm_new_password)) },
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                imageVector = if (confirmVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isLoading,
                onClick = {
                    onSubmit(currentPassword, newPassword, confirmPassword)
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(R.string.auth_change))
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isLoading, onClick = onDismiss) {
                Text(text = stringResource(R.string.auth_back))
            }
        }
    )
}

/**
 * Security status header with visual indicator
 */
@Composable
private fun SecurityStatusHeader(
    isLockEnabled: Boolean,
    isPinSet: Boolean,
    isBiometricEnabled: Boolean
) {
    val statusColor by animateColorAsState(
        targetValue = if (isLockEnabled) SuccessColor else WarningAmber,
        animationSpec = tween(300),
        label = "statusColor"
    )
    
    val securityLevel = when {
        isLockEnabled && isPinSet && isBiometricEnabled -> stringResource(R.string.sec_level_high)
        isLockEnabled && (isPinSet || isBiometricEnabled) -> stringResource(R.string.sec_level_medium)
        isLockEnabled -> stringResource(R.string.sec_level_basic)
        else -> stringResource(R.string.sec_level_inactive)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.M)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLockEnabled) Icons.Outlined.VerifiedUser else Icons.Outlined.GppMaybe,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.sec_status_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isLockEnabled) stringResource(R.string.sec_status_protected) else stringResource(R.string.sec_status_unlocked),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            
            Box(
                modifier = Modifier
                    .clip(AppShapes.S)
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = securityLevel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }
        }
    }
}

/**
 * Section header with emoji
 */
@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SecuritySubsectionLabel(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Enhanced security item with colorful icon
 */
@Composable
private fun EnhancedSecurityItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    trailing: @Composable () -> Unit
) {
    val bgColorAlpha by animateColorAsState(
        targetValue = if (isEnabled) iconColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "bgColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.M)
                    .background(bgColorAlpha),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) SuccessColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        trailing()
    }
}

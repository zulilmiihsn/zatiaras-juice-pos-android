package com.zatiaras.pos.feature.auth.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

// Icon colors for consistent theming
private val LockIconColor = Color(0xFFEC4899) // Pink
private val BiometricIconColor = Color(0xFF8B5CF6) // Purple
private val PinIconColor = Color(0xFF3B82F6) // Blue
private val SuccessColor = Color(0xFF10B981) // Emerald
private val InfoColor = Color(0xFF06B6D4) // Cyan

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
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Keamanan",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
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
                title = "🔐 Kunci Aplikasi",
                subtitle = "Lindungi aplikasi dari akses tidak sah"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Security Card - Enhanced
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    // App Lock Toggle
                    EnhancedSecurityItem(
                        icon = Icons.Outlined.Lock,
                        iconColor = LockIconColor,
                        title = "Kunci Aplikasi",
                        subtitle = if (uiState.lockEnabled) "✓ Aktif - Aplikasi terlindungi" else "Aktifkan kunci saat membuka aplikasi",
                        isEnabled = uiState.lockEnabled,
                        trailing = {
                            Switch(
                                checked = uiState.lockEnabled,
                                onCheckedChange = { viewModel.setLockEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
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
                            title = "Biometric",
                            subtitle = when {
                                !uiState.biometricAvailable -> "⚠️ Tidak tersedia di perangkat ini"
                                uiState.biometricEnabled -> "✓ Aktif - Gunakan sidik jari/wajah"
                                else -> "Gunakan sidik jari atau wajah"
                            },
                            isEnabled = uiState.biometricEnabled,
                            trailing = {
                                Switch(
                                    checked = uiState.biometricEnabled,
                                    onCheckedChange = { viewModel.setBiometricEnabled(it) },
                                    enabled = uiState.biometricAvailable,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BiometricIconColor
                                    )
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // PIN Setting
                        EnhancedSecurityItem(
                            icon = Icons.Outlined.Pin,
                            iconColor = PinIconColor,
                            title = if (uiState.pinSet) "Ubah PIN" else "Atur PIN",
                            subtitle = if (uiState.pinSet) "✓ PIN sudah diatur" else "Atur PIN sebagai cadangan",
                            isEnabled = uiState.pinSet,
                            trailing = {
                                Button(
                                    onClick = onChangePinClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PinIconColor
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        if (uiState.pinSet) "Ubah" else "Atur",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Box - Enhanced
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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
                            .clip(RoundedCornerShape(10.dp))
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
                            text = "Tips Keamanan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InfoColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🔒 Kunci aplikasi melindungi data penjualan dari akses tidak sah.\n" +
                                   "👆 Biometric lebih cepat & aman dari PIN.\n" +
                                   "🔑 PIN sebagai cadangan jika biometric gagal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
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
        targetValue = if (isLockEnabled) SuccessColor else Color(0xFFF59E0B),
        animationSpec = tween(300),
        label = "statusColor"
    )
    
    val securityLevel = when {
        isLockEnabled && isPinSet && isBiometricEnabled -> "Tinggi"
        isLockEnabled && (isPinSet || isBiometricEnabled) -> "Sedang"
        isLockEnabled -> "Dasar"
        else -> "Tidak Aktif"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    .clip(RoundedCornerShape(12.dp))
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
                    text = "Status Keamanan",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isLockEnabled) "Terlindungi" else "Tidak Terkunci",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
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
                    .clip(RoundedCornerShape(12.dp))
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

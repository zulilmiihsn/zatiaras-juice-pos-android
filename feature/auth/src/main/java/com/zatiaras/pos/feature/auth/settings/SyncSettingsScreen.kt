package com.zatiaras.pos.feature.auth.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.ErrorRed
import com.zatiaras.pos.core.ui.theme.InfoBlue
import com.zatiaras.pos.core.ui.theme.PurpleAccent
import com.zatiaras.pos.core.ui.theme.SuccessGreen
import com.zatiaras.pos.core.ui.theme.WarningAmber
import androidx.compose.foundation.border

// Icon colors for consistent theming
private val SyncIconColor = InfoBlue
private val SuccessColor = SuccessGreen
private val WarningColor = WarningAmber
private val ErrorColor = ErrorRed
private val InfoColor = PurpleAccent

/**
 * Sync Settings Sub-Screen - Enhanced with premium styling
 * Contains: Sync status, pending count, sync buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.sync_title),
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
            // Header Description
            Text(
                text = stringResource(R.string.sync_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Sync Status Card - Premium gradient style
            SyncStatusCard(
                lastSyncInfo = uiState.lastSyncInfo,
                pendingCount = uiState.pendingCount,
                isSyncing = uiState.isSyncing
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section Header
            SectionHeader(
                title = stringResource(R.string.sync_action_title),
                subtitle = stringResource(R.string.sync_action_desc)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sync Now Action Card
            SyncActionCard(
                icon = Icons.Outlined.Sync,
                iconColor = SyncIconColor,
                title = stringResource(R.string.sync_now),
                subtitle = stringResource(R.string.sync_now_desc),
                buttonText = if (uiState.isSyncing) stringResource(R.string.sync_button_syncing) else stringResource(R.string.sync_now_btn),
                isLoading = uiState.isSyncing,
                enabled = !uiState.isSyncing,
                onClick = { viewModel.syncNow() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Force Full Sync Card
            SyncActionCard(
                icon = Icons.Outlined.CloudSync,
                iconColor = WarningColor,
                title = stringResource(R.string.sync_full),
                subtitle = stringResource(R.string.sync_full_desc),
                buttonText = stringResource(R.string.sync_full_btn),
                isLoading = false,
                enabled = !uiState.isSyncing,
                isOutlined = true,
                onClick = { viewModel.forceFullSync() }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Section Header for Info
            SectionHeader(
                title = stringResource(R.string.sync_about),
                subtitle = stringResource(R.string.sync_about_desc)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Info Card with bullet points - Enhanced
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.XL,
                colors = CardDefaults.cardColors(
                    containerColor = InfoColor.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(AppShapes.M)
                                .background(InfoColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = InfoColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.sync_how_it_works),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = InfoColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InfoBulletPoint(
                        emoji = "🔄",
                        text = stringResource(R.string.sync_bg_sync)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InfoBulletPoint(
                        emoji = "💾",
                        text = stringResource(R.string.sync_offline_first)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InfoBulletPoint(
                        emoji = "☁️",
                        text = stringResource(R.string.sync_full_desc_info)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InfoBulletPoint(
                        emoji = "⚠️",
                        text = stringResource(R.string.sync_warn)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Premium sync status card with gradient and animations
 */
@Composable
private fun SyncStatusCard(
    lastSyncInfo: LastSyncInfo,
    pendingCount: Int,
    isSyncing: Boolean
) {
    val hasPending = pendingCount > 0
    val statusColor = when {
        isSyncing -> SyncIconColor
        hasPending -> ErrorColor
        else -> SuccessColor
    }
    
    // Rotating animation for sync icon
    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.XXL),
        shape = AppShapes.XXL,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            statusColor,
                            statusColor.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status icon with animation
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isSyncing -> Icons.Outlined.Sync
                                hasPending -> Icons.Outlined.CloudOff
                                else -> Icons.Outlined.CloudDone
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(32.dp)
                                .then(
                                    if (isSyncing) Modifier.rotate(rotation) else Modifier
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = when {
                                isSyncing -> stringResource(R.string.sync_status_syncing)
                                hasPending -> stringResource(R.string.sync_status_pending)
                                else -> stringResource(R.string.sync_status_synced)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = lastSyncInfoText(lastSyncInfo),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                
                Spacer(modifier = Modifier.height(16.dp))

                // Pending Count Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sync_pending_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(AppShapes.M)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sync_pending_count, pendingCount),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun lastSyncInfoText(lastSyncInfo: LastSyncInfo): String {
    return when (lastSyncInfo.unit) {
        LastSyncUnit.NEVER -> stringResource(R.string.sync_never)
        LastSyncUnit.JUST_NOW -> stringResource(R.string.sync_just_now)
        LastSyncUnit.MINUTES_AGO -> stringResource(R.string.sync_mins_ago, lastSyncInfo.value)
        LastSyncUnit.HOURS_AGO -> stringResource(R.string.sync_hours_ago, lastSyncInfo.value)
        LastSyncUnit.DAYS_AGO -> stringResource(R.string.sync_days_ago, lastSyncInfo.value)
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
 * Sync action card with button
 */
@Composable
private fun SyncActionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    buttonText: String,
    isLoading: Boolean,
    enabled: Boolean,
    isOutlined: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.L),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with colored background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.M)
                    .background(iconColor.copy(alpha = 0.1f)),
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
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            if (isOutlined) {
                OutlinedButton(
                    onClick = onClick,
                    enabled = enabled,
                    shape = AppShapes.M
                ) {
                    Text(buttonText, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    shape = AppShapes.M,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = iconColor
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(buttonText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Info bullet point with emoji
 */
@Composable
private fun InfoBulletPoint(
    emoji: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

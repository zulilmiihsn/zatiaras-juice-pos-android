package com.zatiaras.pos.feature.auth.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.auth.R
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.data.access.UserRole
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.Brand600
import com.zatiaras.pos.core.ui.theme.ErrorRed
import com.zatiaras.pos.core.ui.theme.IconColors
import com.zatiaras.pos.core.ui.theme.IndigoAccent
import com.zatiaras.pos.core.ui.theme.InfoBlue
import com.zatiaras.pos.core.ui.theme.Slate500
import com.zatiaras.pos.core.ui.theme.SuccessGreen
import com.zatiaras.pos.core.ui.theme.WarningAmber
import androidx.compose.foundation.border

// Icon background colors for different categories
private val SecurityIconColor = IconColors.Settings
private val AccessIconColor = WarningAmber
private val PrinterIconColor = IconColors.Printer
private val MenuIconColor = IconColors.Store
private val SyncIconColor = InfoBlue
private val AboutIconColor = IconColors.Preview
private val TaxIconColor = ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPrinter: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToAccessControl: () -> Unit = {},
    onNavigateToTransactionHistory: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToSecurity = onNavigateToSecurity,
        onNavigateToAccessControl = onNavigateToAccessControl,
        onNavigateToPrinter = onNavigateToPrinter,
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToTransactionHistory = onNavigateToTransactionHistory,
        onNavigateToSync = onNavigateToSync,
        onNavigateToAbout = onNavigateToAbout,
        onLogoutClick = viewModel::logout,
        onTaxPercentageChange = viewModel::updateTaxPercentage,
        onLowPerformanceModeChange = viewModel::updateLowPerformanceMode
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAccessControl: () -> Unit,
    onNavigateToPrinter: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToTransactionHistory: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogoutClick: () -> Unit,
    onTaxPercentageChange: (Double) -> Unit = {},
    onLowPerformanceModeChange: (Boolean) -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.settings_title),
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
        ) {
            // Premium Profile Card with Gradient
            PremiumProfileCard(
                userEmail = uiState.userEmail,
                userRole = uiState.userRole
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Keamanan & Akses
            SettingsSectionHeader(
                title = stringResource(R.string.settings_sec_access),
                subtitle = stringResource(R.string.settings_sec_desc)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Security Card
            EnhancedSettingsCard(
                icon = Icons.Outlined.Security,
                iconBackgroundColor = SecurityIconColor,
                title = stringResource(R.string.sec_title),
                subtitle = stringResource(R.string.settings_security_subtitle),
                statusBadge = if (uiState.lockEnabled) stringResource(R.string.settings_status_active) else null,
                statusBadgeColor = if (uiState.lockEnabled) SuccessGreen else null,
                onClick = onNavigateToSecurity
            )

            // Access Control (Owner only)
            if (uiState.isOwner) {
                Spacer(modifier = Modifier.height(8.dp))
                EnhancedSettingsCard(
                    icon = Icons.Outlined.AdminPanelSettings,
                    iconBackgroundColor = AccessIconColor,
                    title = stringResource(R.string.access_control_title),
                    subtitle = stringResource(R.string.access_control_desc),
                    statusBadge = stringResource(R.string.user_role_owner),
                    statusBadgeColor = WarningAmber,
                    onClick = onNavigateToAccessControl
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Perangkat & Operasional
            SettingsSectionHeader(
                title = stringResource(R.string.settings_devices),
                subtitle = stringResource(R.string.settings_devices_desc)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Printer
            EnhancedSettingsCard(
                icon = Icons.Outlined.Print,
                iconBackgroundColor = PrinterIconColor,
                title = stringResource(R.string.settings_printer),
                subtitle = stringResource(R.string.settings_printer_desc),
                onClick = onNavigateToPrinter
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Inventory/Menu
            EnhancedSettingsCard(
                icon = Icons.Outlined.Restaurant,
                iconBackgroundColor = MenuIconColor,
                title = stringResource(R.string.settings_menu),
                subtitle = stringResource(R.string.settings_menu_desc),
                onClick = onNavigateToInventory
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Laporan
            SettingsSectionHeader(
                title = stringResource(R.string.settings_reports_title),
                subtitle = stringResource(R.string.settings_reports_desc)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tax Percentage
            TaxPercentageCard(
                currentPercentage = uiState.taxPercentage,
                onPercentageChange = onTaxPercentageChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Data & Informasi
            SettingsSectionHeader(
                title = stringResource(R.string.settings_data_info),
                subtitle = stringResource(R.string.settings_sync_desc)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sync
            EnhancedSettingsCard(
                icon = Icons.Outlined.Sync,
                iconBackgroundColor = SyncIconColor,
                title = stringResource(R.string.settings_sync),
                subtitle = lastSyncInfoText(uiState.lastSyncInfo),
                onClick = onNavigateToSync
            )

            Spacer(modifier = Modifier.height(8.dp))

            // History
            EnhancedSettingsCard(
                icon = Icons.Outlined.History,
                iconBackgroundColor = IndigoAccent,
                title = stringResource(R.string.settings_history_title),
                subtitle = stringResource(R.string.settings_history_desc),
                onClick = onNavigateToTransactionHistory
            )

            Spacer(modifier = Modifier.height(8.dp))

            EnhancedSettingsCard(
                icon = Icons.Outlined.Info,
                iconBackgroundColor = AboutIconColor,
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_about_version, "1.0"),
                onClick = onNavigateToAbout
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Performa (New)
            SettingsSectionHeader(
                title = stringResource(R.string.settings_performance_title),
                subtitle = stringResource(R.string.settings_performance_desc)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PerformanceModeCard(
                enabled = uiState.lowPerformanceMode,
                onEnabledChange = onLowPerformanceModeChange
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button - More prominent and friendly
            LogoutButton(onClick = onLogoutClick)

            Spacer(modifier = Modifier.height(32.dp))
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
 * Section header with emoji and subtitle for better visual hierarchy
 */
@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
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
 * Enhanced settings card with colorful icon background and status badge
 */
@Composable
private fun EnhancedSettingsCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    title: String,
    subtitle: String,
    statusBadge: String? = null,
    statusBadgeColor: Color? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
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
            // Colorful icon background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.M)
                    .background(iconBackgroundColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBackgroundColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Status badge
                    if (statusBadge != null && statusBadgeColor != null) {
                        Box(
                            modifier = Modifier
                                .clip(AppShapes.S)
                                .background(statusBadgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusBadgeColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Premium profile card with gradient background - Ultra Simple
 */
@Composable
private fun PremiumProfileCard(
    userEmail: String,
    userRole: UserRole
) {
    val isOwner = userRole.isOwner()
    val userRoleLabel = when (userRole) {
        UserRole.PEMILIK -> stringResource(R.string.user_role_owner)
        UserRole.KASIR -> stringResource(R.string.user_role_cashier)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.L),
        shape = AppShapes.L,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Brand500,
                            Brand600
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOwner) {
                        Text(
                            text = "👑",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = userRoleLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
            
            // Minimalist icon for the role
            Icon(
                imageVector = if (isOwner) Icons.Default.Stars else Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * Friendly logout button with confirmation feel
 */
@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.settings_logout),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Tax percentage setting card with inline editing
 */
@Composable
private fun TaxPercentageCard(
    currentPercentage: Double,
    onPercentageChange: (Double) -> Unit
) {
    // Format nicely: avoid trailing zeros for whole numbers, keep decimals as-is
    fun formatPercentage(value: Double): String {
        return if (value == value.toLong().toDouble()) value.toLong().toString()
        else value.toBigDecimal().stripTrailingZeros().toPlainString()
    }

    var taxInput by remember(currentPercentage) { mutableStateOf(formatPercentage(currentPercentage)) }
    val isDirty by remember(taxInput, currentPercentage) {
        derivedStateOf {
            val parsed = taxInput.replace(",", ".").toDoubleOrNull()
            parsed != null && parsed >= 0 && parsed != currentPercentage
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.L),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.M)
                    .background(TaxIconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Receipt,
                    contentDescription = null,
                    tint = TaxIconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Label
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_tax_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.settings_tax_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Input + Save row
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = taxInput,
                    onValueChange = { taxInput = it },
                    modifier = Modifier
                        .width(72.dp)
                        .height(56.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val newTax = taxInput.replace(",", ".").toDoubleOrNull()
                            if (newTax != null && newTax >= 0) {
                                onPercentageChange(newTax)
                            }
                        }
                    ),
                    shape = AppShapes.M,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDirty) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = if (isDirty) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                               else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Save button — only visible when value is changed
                androidx.compose.animation.AnimatedVisibility(visible = isDirty) {
                    Row {
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val newTax = taxInput.replace(",", ".").toDoubleOrNull()
                                if (newTax != null && newTax >= 0) {
                                    onPercentageChange(newTax)
                                }
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .width(56.dp),
                            shape = AppShapes.M,
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.settings_save),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card to toggle Low Performance Mode
 */
@Composable
private fun PerformanceModeCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.L),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .background(Slate500.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_low_perf_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.settings_low_perf_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

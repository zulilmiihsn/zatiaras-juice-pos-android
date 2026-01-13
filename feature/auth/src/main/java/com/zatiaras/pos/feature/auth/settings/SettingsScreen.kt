package com.zatiaras.pos.feature.auth.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.data.access.LockableRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPinSetup: () -> Unit,
    onNavigateToPrinter: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
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
        onLockEnabledChange = viewModel::setLockEnabled,
        onBiometricEnabledChange = viewModel::setBiometricEnabled,
        onChangePinClick = onNavigateToPinSetup,
        onPrinterClick = onNavigateToPrinter,
        onInventoryClick = onNavigateToInventory,
        onSyncNowClick = viewModel::syncNow,
        onForceFullSyncClick = viewModel::forceFullSync,
        onLogoutClick = viewModel::logout,
        onToggleRouteLock = viewModel::toggleRouteLock,
        onSetOwnerPin = viewModel::setOwnerPin
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onLockEnabledChange: (Boolean) -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onChangePinClick: () -> Unit,
    onPrinterClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onForceFullSyncClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onToggleRouteLock: (LockableRoute) -> Unit = {},
    onSetOwnerPin: (String) -> Unit = {}
) {
    // State for owner PIN setup dialog
    var showOwnerPinDialog by remember { mutableStateOf(false) }
    var ownerPin by remember { mutableStateOf("") }
    var ownerPinConfirm by remember { mutableStateOf("") }
    var ownerPinError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Section
            SettingsSection(title = "Profil") {
                ProfileCard(
                    userName = uiState.userName,
                    userEmail = uiState.userEmail,
                    userRole = uiState.userRole
                )
            }

            HorizontalDivider()

            // Security Section
            SettingsSection(title = "Keamanan") {
                SwitchSettingItem(
                    icon = Icons.Outlined.Lock,
                    title = "Kunci Aplikasi",
                    subtitle = "Aktifkan kunci saat membuka aplikasi",
                    checked = uiState.lockEnabled,
                    onCheckedChange = onLockEnabledChange
                )

                if (uiState.lockEnabled) {
                    SwitchSettingItem(
                        icon = Icons.Outlined.Fingerprint,
                        title = "Biometric",
                        subtitle = if (uiState.biometricAvailable) {
                            "Gunakan sidik jari atau wajah"
                        } else {
                            "Tidak tersedia di perangkat ini"
                        },
                        checked = uiState.biometricEnabled,
                        onCheckedChange = onBiometricEnabledChange,
                        enabled = uiState.biometricAvailable
                    )

                    ClickableSettingItem(
                        icon = Icons.Outlined.Pin,
                        title = if (uiState.pinSet) "Ubah PIN" else "Atur PIN",
                        subtitle = if (uiState.pinSet) "PIN sudah diatur" else "Atur PIN sebagai cadangan",
                        onClick = onChangePinClick
                    )
                }
            }

            // Access Control Section (Owner only)
            if (uiState.isOwner) {
                HorizontalDivider()

                SettingsSection(title = "Kunci Menu (Kontrol Akses)") {
                    // Owner PIN Setup
                    ClickableSettingItem(
                        icon = Icons.Outlined.Key,
                        title = if (uiState.ownerPinSet) "Ubah PIN Pemilik" else "Atur PIN Pemilik",
                        subtitle = if (uiState.ownerPinSet) {
                            "PIN untuk akses menu terkunci"
                        } else {
                            "Atur PIN agar kasir bisa akses menu terkunci"
                        },
                        onClick = { showOwnerPinDialog = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = "Menu yang dikunci akan meminta PIN pemilik saat diakses oleh kasir:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Lockable routes toggles
                    uiState.lockableRoutes.forEach { (route, isLocked) ->
                        SwitchSettingItem(
                            icon = if (isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            title = route.displayName,
                            subtitle = if (isLocked) "Dikunci - Kasir perlu PIN" else "Tidak dikunci",
                            checked = isLocked,
                            onCheckedChange = { onToggleRouteLock(route) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Hardware Section
            SettingsSection(title = "Perangkat") {
                ClickableSettingItem(
                    icon = Icons.Outlined.Print,
                    title = "Printer",
                    subtitle = "Atur printer thermal Bluetooth",
                    onClick = onPrinterClick
                )
            }

            HorizontalDivider()

            // Inventory/Menu Section
            SettingsSection(title = "Manajemen") {
                ClickableSettingItem(
                    icon = Icons.Outlined.Restaurant,
                    title = "Kelola Menu",
                    subtitle = "Tambah, edit, atau hapus produk",
                    onClick = onInventoryClick
                )
            }

            HorizontalDivider()

            // Sync Section
            SettingsSection(title = "Sinkronisasi") {
                InfoSettingItem(
                    icon = Icons.Outlined.Sync,
                    title = "Status Sync",
                    value = uiState.lastSyncInfo
                )

                InfoSettingItem(
                    icon = Icons.Outlined.CloudQueue,
                    title = "Data Pending",
                    value = "${uiState.pendingCount} item belum tersinkron"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onSyncNowClick,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSyncing
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Sync Sekarang")
                    }

                    TextButton(
                        onClick = onForceFullSyncClick,
                        enabled = !uiState.isSyncing
                    ) {
                        Text("Force Full Sync")
                    }
                }
            }

            HorizontalDivider()

            // About Section
            SettingsSection(title = "Tentang") {
                InfoSettingItem(
                    icon = Icons.Outlined.Info,
                    title = "Versi Aplikasi",
                    value = "1.0.0"
                )

                InfoSettingItem(
                    icon = Icons.Outlined.Store,
                    title = "Cabang",
                    value = uiState.branchName
                )
            }

            HorizontalDivider()

            // Logout
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Owner PIN Setup Dialog
    if (showOwnerPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showOwnerPinDialog = false
                ownerPin = ""
                ownerPinConfirm = ""
                ownerPinError = null
            },
            title = {
                Text(
                    text = if (uiState.ownerPinSet) "Ubah PIN Pemilik" else "Atur PIN Pemilik",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "PIN ini akan diminta saat kasir mengakses menu yang dikunci.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = ownerPin,
                        onValueChange = { 
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                ownerPin = it
                                ownerPinError = null
                            }
                        },
                        label = { Text("PIN Baru (4 digit)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ownerPinConfirm,
                        onValueChange = { 
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                ownerPinConfirm = it
                                ownerPinError = null
                            }
                        },
                        label = { Text("Konfirmasi PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = ownerPinError != null
                    )

                    if (ownerPinError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ownerPinError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            ownerPin.length != 4 -> {
                                ownerPinError = "PIN harus 4 digit"
                            }
                            ownerPin != ownerPinConfirm -> {
                                ownerPinError = "PIN tidak cocok"
                            }
                            else -> {
                                onSetOwnerPin(ownerPin)
                                showOwnerPinDialog = false
                                ownerPin = ""
                                ownerPinConfirm = ""
                                ownerPinError = null
                            }
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOwnerPinDialog = false
                        ownerPin = ""
                        ownerPinConfirm = ""
                        ownerPinError = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun ProfileCard(
    userName: String,
    userEmail: String,
    userRole: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    .size(56.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName.ifEmpty { "User" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            AssistChip(
                onClick = { },
                label = { Text(userRole) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
private fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        },
        headlineContent = {
            Text(
                text = title,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

@Composable
private fun ClickableSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun InfoSettingItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = { Text(title) },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

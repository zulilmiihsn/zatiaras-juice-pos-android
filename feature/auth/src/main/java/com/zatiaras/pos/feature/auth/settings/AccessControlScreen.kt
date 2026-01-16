package com.zatiaras.pos.feature.auth.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zatiaras.pos.core.data.access.LockableRoute

/**
 * Access Control Settings Sub-Screen (Owner Only)
 * Contains: Owner PIN setup, Route lock toggles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessControlScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showOwnerPinDialog by remember { mutableStateOf(false) }
    var ownerPin by remember { mutableStateOf("") }
    var ownerPinConfirm by remember { mutableStateOf("") }
    var ownerPinError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Kontrol Akses",
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
                }
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
            // Description
            Text(
                text = "Kelola akses menu untuk kasir. Menu yang dikunci akan meminta PIN pemilik.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Owner PIN Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (uiState.ownerPinSet) "Ubah PIN Pemilik" else "Atur PIN Pemilik",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (uiState.ownerPinSet) {
                                    "PIN untuk akses menu terkunci"
                                } else {
                                    "Atur PIN agar kasir bisa akses menu terkunci"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(onClick = { showOwnerPinDialog = true }) {
                        Text(if (uiState.ownerPinSet) "Ubah" else "Atur")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lockable Routes Section
            Text(
                text = "Menu yang Dapat Dikunci",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    uiState.lockableRoutes.forEachIndexed { index, (route, isLocked) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                    contentDescription = null,
                                    tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = route.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isLocked) "Dikunci - Kasir perlu PIN" else "Tidak dikunci",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isLocked,
                                onCheckedChange = { viewModel.toggleRouteLock(route) }
                            )
                        }
                        if (index < uiState.lockableRoutes.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Kasir dapat mengakses menu yang dikunci dengan memasukkan PIN pemilik.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
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
                                viewModel.setOwnerPin(ownerPin)
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

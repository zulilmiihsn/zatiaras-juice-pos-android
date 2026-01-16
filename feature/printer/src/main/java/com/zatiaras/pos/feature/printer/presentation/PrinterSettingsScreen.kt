package com.zatiaras.pos.feature.printer.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.feature.printer.domain.model.PaperWidth
import com.zatiaras.pos.feature.printer.domain.model.PrinterDevice
import com.zatiaras.pos.feature.printer.domain.model.PrinterStatus
import com.zatiaras.pos.core.ui.theme.LocalDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: PrinterSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Bluetooth enable launcher
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onBluetoothEnabled()
        }
    }
    
    // Permission launcher for Android 12+
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.onBluetoothPermissionGranted()
        } else {
            Toast.makeText(context, "Izin Bluetooth diperlukan", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PrinterEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is PrinterEvent.RequestBluetoothEnable -> {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(intent)
                }
                is PrinterEvent.RequestBluetoothPermissions -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                            )
                        )
                    }
                }
                is PrinterEvent.PrintComplete -> {
                    Toast.makeText(context, "Cetak berhasil!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Check permissions on launch
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            viewModel.refreshPairedDevices()
        }
    }
    
    PrinterSettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRefreshDevices = viewModel::refreshPairedDevices,
        onConnectDevice = viewModel::connectToPrinter,
        onDisconnect = viewModel::disconnect,
        onPrintTest = viewModel::printTestPage,
        onPaperWidthChange = viewModel::setPaperWidth,
        onStoreNameChange = viewModel::setStoreName,
        onStoreAddressChange = viewModel::setStoreAddress,
        onSaveStoreInfo = viewModel::saveStoreInfo,
        onAutoConnectChange = viewModel::setAutoConnect
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterSettingsScreen(
    uiState: PrinterSettingsUiState,
    onNavigateBack: () -> Unit,
    onRefreshDevices: () -> Unit,
    onConnectDevice: (PrinterDevice) -> Unit,
    onDisconnect: () -> Unit,
    onPrintTest: () -> Unit,
    onPaperWidthChange: (PaperWidth) -> Unit,
    onStoreNameChange: (String) -> Unit,
    onStoreAddressChange: (String) -> Unit,
    onSaveStoreInfo: () -> Unit,
    onAutoConnectChange: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pengaturan Printer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshDevices) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        val dimensions = LocalDimensions.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(dimensions.paddingM),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingM)
        ) {
            // Connection Status Card
            item {
                ConnectionStatusCard(
                    status = uiState.printerStatus,
                    statusMessage = uiState.statusMessage,
                    onDisconnect = onDisconnect,
                    onPrintTest = onPrintTest
                )
            }
            
            // Paired Devices Section
            item {
                Text(
                    text = "Perangkat Tersedia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (uiState.pairedDevices.isEmpty()) {
                item {
                    EmptyDevicesCard(isBluetoothEnabled = uiState.isBluetoothEnabled)
                }
            } else {
                items(uiState.pairedDevices) { device ->
                    PrinterDeviceItem(
                        device = device,
                        isConnecting = uiState.isConnecting && uiState.selectedDevice?.address == device.address,
                        onClick = { onConnectDevice(device) }
                    )
                }
            }
            
            // Settings Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Paper Width
            item {
                PaperWidthSelector(
                    selected = uiState.paperWidth,
                    onSelect = onPaperWidthChange
                )
            }
            
            // Auto Connect
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    val dimensions = LocalDimensions.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensions.paddingM),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Connect",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Hubungkan otomatis ke printer terakhir",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.autoConnect,
                            onCheckedChange = onAutoConnectChange
                        )
                    }
                }
            }
            
            // Store Info Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Info Toko (Header Struk)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                StoreInfoCard(
                    storeName = uiState.storeName,
                    storeAddress = uiState.storeAddress,
                    onStoreNameChange = onStoreNameChange,
                    onStoreAddressChange = onStoreAddressChange,
                    onSave = onSaveStoreInfo
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    status: PrinterStatus,
    statusMessage: String,
    onDisconnect: () -> Unit,
    onPrintTest: () -> Unit
) {
    val isConnected = status is PrinterStatus.Connected || 
                      status is PrinterStatus.Printing ||
                      status is PrinterStatus.PrintSuccess
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        val dimensions = LocalDimensions.current
        Column(
            modifier = Modifier.padding(dimensions.paddingM)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        isConnected -> Icons.Default.BluetoothConnected
                        status is PrinterStatus.Connecting -> Icons.Default.Bluetooth
                        else -> Icons.Default.BluetoothDisabled
                    },
                    contentDescription = null,
                    tint = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isConnected) "Terhubung" else "Status Printer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (status is PrinterStatus.Connecting || status is PrinterStatus.Printing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            if (isConnected) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingS)
                ) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Putuskan")
                    }
                    
                    Button(
                        onClick = onPrintTest,
                        modifier = Modifier.weight(1f),
                        enabled = status !is PrinterStatus.Printing
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Print")
                    }
                }
            }
        }
    }
}

@Composable
private fun PrinterDeviceItem(
    device: PrinterDevice,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting && !device.isConnected, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isConnected) 
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        val dimensions = LocalDimensions.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (device.isLikelyPrinter) Icons.Default.Print else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (device.isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            when {
                device.isConnected -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Connected",
                        tint = Color(0xFF4CAF50)
                    )
                }
                isConnecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDevicesCard(isBluetoothEnabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        val dimensions = LocalDimensions.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingXL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isBluetoothEnabled) Icons.Default.Print else Icons.Default.BluetoothDisabled,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isBluetoothEnabled) 
                    "Tidak ada printer ditemukan" 
                else 
                    "Bluetooth tidak aktif",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isBluetoothEnabled)
                    "Pastikan printer sudah dipasangkan di Settings Bluetooth"
                else
                    "Aktifkan Bluetooth untuk mencari printer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaperWidthSelector(
    selected: PaperWidth,
    onSelect: (PaperWidth) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        val dimensions = LocalDimensions.current
        Column(
            modifier = Modifier.padding(dimensions.paddingM)
        ) {
            Text(
                text = "Lebar Kertas",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = selected == PaperWidth.MM_58,
                    onClick = { onSelect(PaperWidth.MM_58) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("58mm")
                }
                SegmentedButton(
                    selected = selected == PaperWidth.MM_80,
                    onClick = { onSelect(PaperWidth.MM_80) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("80mm")
                }
            }
        }
    }
}

@Composable
private fun StoreInfoCard(
    storeName: String,
    storeAddress: String,
    onStoreNameChange: (String) -> Unit,
    onStoreAddressChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        val dimensions = LocalDimensions.current
        Column(
            modifier = Modifier.padding(dimensions.paddingM)
        ) {
            OutlinedTextField(
                value = storeName,
                onValueChange = onStoreNameChange,
                label = { Text("Nama Toko") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = storeAddress,
                onValueChange = onStoreAddressChange,
                label = { Text("Alamat (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSave,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Simpan")
            }
        }
    }
}

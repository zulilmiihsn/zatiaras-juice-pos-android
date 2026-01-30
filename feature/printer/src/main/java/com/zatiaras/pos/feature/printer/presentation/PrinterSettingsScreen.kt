package com.zatiaras.pos.feature.printer.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Store
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zatiaras.pos.feature.printer.domain.model.PaperWidth
import com.zatiaras.pos.feature.printer.domain.model.PrinterDevice
import com.zatiaras.pos.feature.printer.domain.model.PrinterStatus
import com.zatiaras.pos.core.ui.theme.LocalDimensions

// Icon colors
private val PrinterIconColor = Color(0xFF10B981) // Emerald
private val SettingsIconColor = Color(0xFF6366F1) // Indigo  
private val StoreIconColor = Color(0xFFEC4899) // Pink
private val PreviewIconColor = Color(0xFF8B5CF6) // Purple
private val LogoIconColor = Color(0xFFF59E0B) // Amber

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
        onStoreLogoChange = viewModel::setStoreLogo,
        onClearStoreLogo = viewModel::clearStoreLogo,
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
    onStoreLogoChange: (String?) -> Unit,
    onClearStoreLogo: () -> Unit,
    onSaveStoreInfo: () -> Unit,
    onAutoConnectChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistent permission so the URI remains valid
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Some providers don't support persistent permissions
            }
            onStoreLogoChange(it.toString())
        }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Pengaturan Printer",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshDevices) {
                        Icon(
                            Icons.Default.Refresh, 
                            "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card - Prominent
            item {
                EnhancedConnectionStatusCard(
                    status = uiState.printerStatus,
                    statusMessage = uiState.statusMessage,
                    onDisconnect = onDisconnect,
                    onPrintTest = onPrintTest
                )
            }
            
            // Section: Perangkat Bluetooth
            item {
                SectionHeader(
                    title = "🔌 Perangkat Bluetooth",
                    subtitle = "Pilih printer thermal untuk cetak struk"
                )
            }
            
            // Auto Connect toggle - now in Bluetooth section
            item {
                EnhancedToggleCard(
                    icon = Icons.Default.Bluetooth,
                    iconColor = Color(0xFF3B82F6),
                    title = "Hubung Otomatis",
                    subtitle = "Sambungkan ke printer terakhir saat buka aplikasi",
                    checked = uiState.autoConnect,
                    onCheckedChange = onAutoConnectChange
                )
            }
            
            if (uiState.pairedDevices.isEmpty()) {
                item {
                    EnhancedEmptyDevicesCard(isBluetoothEnabled = uiState.isBluetoothEnabled)
                }
            } else {
                items(uiState.pairedDevices) { device ->
                    EnhancedPrinterDeviceItem(
                        device = device,
                        isConnecting = uiState.isConnecting && uiState.selectedDevice?.address == device.address,
                        onClick = { onConnectDevice(device) }
                    )
                }
            }
            
            // Section: Pengaturan Cetak
            item {
                SectionHeader(
                    title = "⚙️ Pengaturan Cetak",
                    subtitle = "Atur lebar kertas thermal printer"
                )
            }
            
            // Paper Width & Auto Connect
            item {
                EnhancedSettingsCard(
                    icon = Icons.Outlined.Print,
                    iconColor = SettingsIconColor,
                    title = "Lebar Kertas",
                    content = {
                        Spacer(modifier = Modifier.height(12.dp))
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = uiState.paperWidth == PaperWidth.MM_58,
                                onClick = { onPaperWidthChange(PaperWidth.MM_58) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("58mm (Kecil)")
                            }
                            SegmentedButton(
                                selected = uiState.paperWidth == PaperWidth.MM_80,
                                onClick = { onPaperWidthChange(PaperWidth.MM_80) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("80mm (Besar)")
                            }
                        }
                    }
                )
            }
            
            // Section: Info Toko
            item {
                SectionHeader(
                    title = "🏪 Info Toko",
                    subtitle = "Atur informasi yang tampil di header struk"
                )
            }
            
            // Store Logo
            item {
                StoreLogoCard(
                    logoUri = uiState.storeLogoUri,
                    onSelectLogo = { imagePickerLauncher.launch("image/*") },
                    onClearLogo = onClearStoreLogo
                )
            }
            
            item {
                EnhancedStoreInfoCard(
                    storeName = uiState.storeName,
                    storeAddress = uiState.storeAddress,
                    onStoreNameChange = onStoreNameChange,
                    onStoreAddressChange = onStoreAddressChange,
                    onSave = onSaveStoreInfo
                )
            }
            
            // Section: Preview Struk
            item {
                SectionHeader(
                    title = "🧾 Preview Struk",
                    subtitle = "Lihat bagaimana struk akan tercetak"
                )
            }
            
            item {
                ReceiptPreviewCard(
                    storeName = uiState.storeName,
                    storeAddress = uiState.storeAddress,
                    storeLogoUri = uiState.storeLogoUri,
                    paperWidth = uiState.paperWidth
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
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
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
private fun EnhancedConnectionStatusCard(
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                Color(0xFF10B981).copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isConnected) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon with background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isConnected) Color(0xFF10B981).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isConnected -> Icons.Default.BluetoothConnected
                            status is PrinterStatus.Connecting -> Icons.Default.Bluetooth
                            else -> Icons.Default.BluetoothDisabled
                        },
                        contentDescription = null,
                        tint = if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isConnected) "✓ Printer Terhubung" else "Printer Tidak Terhubung",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (status is PrinterStatus.Connecting || status is PrinterStatus.Printing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (isConnected) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Putuskan")
                    }
                    
                    Button(
                        onClick = onPrintTest,
                        modifier = Modifier.weight(1f),
                        enabled = status !is PrinterStatus.Printing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        )
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
private fun EnhancedPrinterDeviceItem(
    device: PrinterDevice,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting && !device.isConnected, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isConnected) 
                Color(0xFF10B981).copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (device.isConnected) Color(0xFF10B981).copy(alpha = 0.15f)
                        else PrinterIconColor.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (device.isLikelyPrinter) Icons.Default.Print else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (device.isConnected) Color(0xFF10B981) else PrinterIconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            when {
                device.isConnected -> {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Connected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                isConnecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedEmptyDevicesCard(isBluetoothEnabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBluetoothEnabled) Icons.Default.Print else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isBluetoothEnabled) 
                    "Tidak Ada Printer Ditemukan" 
                else 
                    "Bluetooth Tidak Aktif",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isBluetoothEnabled)
                    "Pastikan printer sudah dipasangkan\ndi Pengaturan Bluetooth HP"
                else
                    "Aktifkan Bluetooth untuk\nmencari printer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EnhancedSettingsCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
private fun EnhancedToggleCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
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
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * Card for selecting store logo
 */
@Composable
private fun StoreLogoCard(
    logoUri: String?,
    onSelectLogo: () -> Unit,
    onClearLogo: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LogoIconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = LogoIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Logo Toko",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (logoUri == null) "Menggunakan logo default" else "Logo custom dipilih",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Logo preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            2.dp,
                            if (logoUri != null) StoreIconColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(logoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Logo Toko",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Default app icon
                        Image(
                            painter = painterResource(id = com.zatiaras.pos.core.ui.R.drawable.zatiaras_logo),
                            contentDescription = "Logo Default",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (logoUri != null) {
                    OutlinedButton(
                        onClick = onClearLogo,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hapus")
                    }
                }
                
                Button(
                    onClick = onSelectLogo,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LogoIconColor
                    )
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (logoUri == null) "Pilih Logo" else "Ganti")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "💡 Logo akan tampil di bagian atas struk. Gunakan gambar dengan background transparan untuk hasil terbaik.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EnhancedStoreInfoCard(
    storeName: String,
    storeAddress: String,
    onStoreNameChange: (String) -> Unit,
    onStoreAddressChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StoreIconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Store,
                        contentDescription = null,
                        tint = StoreIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Informasi Toko",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tampil di bagian atas struk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Store Name
            OutlinedTextField(
                value = storeName,
                onValueChange = onStoreNameChange,
                label = { Text("Nama Toko") },
                placeholder = { Text("Contoh: Warung Makan Barokah") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Store Address
            OutlinedTextField(
                value = storeAddress,
                onValueChange = onStoreAddressChange,
                label = { Text("Alamat Toko (Opsional)") },
                placeholder = { Text("Contoh: Jl. Merdeka No. 123") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StoreIconColor
                )
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Info Toko")
            }
        }
    }
}

/**
 * Premium Receipt Preview that simulates a high-end minimarket receipt
 */
@Composable
private fun ReceiptPreviewCard(
    storeName: String,
    storeAddress: String,
    storeLogoUri: String?,
    paperWidth: PaperWidth
) {
    val context = LocalContext.current
    val receiptWidth = if (paperWidth == PaperWidth.MM_58) 240.dp else 300.dp
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PreviewIconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Receipt,
                        contentDescription = null,
                        tint = PreviewIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Preview Struk",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tampilan struk ${if (paperWidth == PaperWidth.MM_58) "58mm" else "80mm"} • Premium",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Premium Receipt
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PremiumReceipt(
                    storeName = storeName,
                    storeAddress = storeAddress,
                    storeLogoUri = storeLogoUri,
                    receiptWidth = receiptWidth,
                    context = context
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Info text
            Text(
                text = "💡 Ini adalah simulasi visual. Hasil cetak sebenarnya menggunakan format ESC/POS thermal printer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PremiumReceipt(
    storeName: String,
    storeAddress: String,
    storeLogoUri: String?,
    receiptWidth: androidx.compose.ui.unit.Dp,
    context: android.content.Context
) {
    // Thermal print colors - only black and gray on thermal paper
    val textBlack = Color(0xFF1A1A1A)
    val textGray = Color(0xFF555555)
    val textLightGray = Color(0xFF888888)
    val dividerColor = Color(0xFF444444)
    
    // Receipt paper with shadow
    Card(
        modifier = Modifier.width(receiptWidth),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    // Thermal paper yellowish tint
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFEFC),
                            Color(0xFFFFFDF8),
                            Color(0xFFFFFCF0)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ===== HEADER SECTION =====
            
            // Store Logo (grayscale effect)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                if (storeLogoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(storeLogoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Logo Toko",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = com.zatiaras.pos.core.ui.R.drawable.zatiaras_logo),
                        contentDescription = "Logo Default",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Store name - Bold black
            Text(
                text = storeName.uppercase().ifEmpty { "NAMA TOKO" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                color = textBlack
            )
            
            // Store address
            if (storeAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = storeAddress,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = textGray,
                    lineHeight = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Double line divider
            Column {
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Transaction info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "No. Transaksi",
                        style = MaterialTheme.typography.labelSmall,
                        color = textLightGray
                    )
                    Text(
                        text = "TRX-2024-001234",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textBlack
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Tanggal",
                        style = MaterialTheme.typography.labelSmall,
                        color = textLightGray
                    )
                    Text(
                        text = "30 Jan 2026, 14:35",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textBlack
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Dashed divider
            Text(
                text = "- - - - - - - - - - - - - - - - - - - -",
                style = MaterialTheme.typography.labelSmall,
                color = dividerColor,
                letterSpacing = 0.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // ===== ITEMS SECTION =====
            
            // Item rows
            ReceiptItemMono(
                name = "Es Teh Manis",
                qty = 1,
                price = 5000,
                textColor = textBlack,
                subTextColor = textLightGray
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            ReceiptItemMono(
                name = "Nasi Goreng Spesial",
                qty = 2,
                price = 30000,
                textColor = textBlack,
                subTextColor = textLightGray
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            ReceiptItemMono(
                name = "Ayam Bakar Madu",
                qty = 1,
                price = 25000,
                textColor = textBlack,
                subTextColor = textLightGray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Dashed divider
            Text(
                text = "- - - - - - - - - - - - - - - - - - - -",
                style = MaterialTheme.typography.labelSmall,
                color = dividerColor,
                letterSpacing = 0.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // ===== TOTALS SECTION =====
            
            // Subtotal
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = textGray)
                Text("Rp 60.000", style = MaterialTheme.typography.bodySmall, color = textBlack)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Tax
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pajak (10%)", style = MaterialTheme.typography.bodySmall, color = textLightGray)
                Text("Rp 6.000", style = MaterialTheme.typography.bodySmall, color = textLightGray)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Solid divider
            HorizontalDivider(thickness = 1.dp, color = dividerColor)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Grand Total - Bold and prominent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = textBlack
                )
                Text(
                    text = "Rp 66.000",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = textBlack
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Payment info - simple text format (like real receipt)
            HorizontalDivider(thickness = 1.dp, color = dividerColor)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bayar (Tunai)", style = MaterialTheme.typography.bodySmall, color = textBlack)
                Text("Rp 100.000", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = textBlack)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kembali", style = MaterialTheme.typography.bodySmall, color = textBlack)
                Text("Rp 34.000", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = textBlack)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Double line divider
            Column {
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ===== FOOTER SECTION =====
            
            // Thank you message
            Text(
                text = "*** Terima Kasih ***",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textBlack
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Selamat Menikmati",
                style = MaterialTheme.typography.bodySmall,
                color = textGray
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Barang yang sudah dibeli",
                style = MaterialTheme.typography.labelSmall,
                color = textLightGray
            )
            Text(
                text = "tidak dapat ditukar/dikembalikan",
                style = MaterialTheme.typography.labelSmall,
                color = textLightGray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Powered by
            Text(
                text = "- - - - - - - - - - - - - - - - - - - -",
                style = MaterialTheme.typography.labelSmall,
                color = dividerColor.copy(alpha = 0.5f),
                letterSpacing = 0.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Powered by ZATIARAS POS",
                style = MaterialTheme.typography.labelSmall,
                color = textLightGray
            )
        }
    }
}

/**
 * Receipt item row for thermal printer style output
 */
@Composable
private fun ReceiptItemMono(
    name: String,
    qty: Int,
    price: Int,
    textColor: Color,
    subTextColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(price)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
        Text(
            text = "${qty}x @ Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(price / qty)}",
            style = MaterialTheme.typography.labelSmall,
            color = subTextColor
        )
    }
}


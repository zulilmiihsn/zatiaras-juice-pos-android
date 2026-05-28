package com.zatiaras.pos.feature.printer.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.IconColors
import com.zatiaras.pos.feature.printer.R
import com.zatiaras.pos.feature.printer.domain.model.PaperWidth
import com.zatiaras.pos.feature.printer.domain.model.PrinterDevice
import com.zatiaras.pos.feature.printer.domain.model.PrinterStatus

// Keep printer-specific icon aliases here so reusable cards can stay theme
// driven without knowing which settings section they render.
private val SettingsIconColor = IconColors.Settings

/**
 * Wires Android Bluetooth permissions, enable intents, and ViewModel events.
 *
 * PrinterSettingsScreen below is intentionally platform-light; this route owns
 * ActivityResult launchers and Toast side effects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: PrinterSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Bluetooth enable is a system activity result, not a ViewModel concern.
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onBluetoothEnabled()
        }
    }

    // Android 12+ requires runtime Bluetooth permissions before scanning or
    // connecting to paired devices.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.onBluetoothPermissionGranted()
        } else {
            Toast.makeText(context, context.getString(R.string.printer_bluetooth_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    // Collect one-off events once per route instance. The ViewModel exposes
    // intent-like events so Android APIs stay out of business state.
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
                                Manifest.permission.BLUETOOTH_SCAN,
                            ),
                        )
                    }
                }
                is PrinterEvent.PrintComplete -> {
                    Toast.makeText(context, context.getString(R.string.printer_print_success), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Initial device list depends on permission status. Older Android versions
    // can refresh immediately because paired-device access is install-time.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                ),
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
        onAutoConnectChange = viewModel::setAutoConnect,
    )
}

/**
 * Printer settings content and section ordering.
 *
 * Keep receipt branding, paper width, device list, and preview in one screen so
 * operators can verify the selected device and printed output together.
 */
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
    onAutoConnectChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    // Store logo uses a persistable URI where providers allow it, otherwise the
    // app still stores the selected URI and lets downstream loading handle it.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            // Take persistent permission so the logo remains readable after the
            // picker activity closes or the app restarts.
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (e: SecurityException) {
                // Some providers do not support persistable permissions.
                // Selection should still succeed for the current URI.
            }
            onStoreLogoChange(it.toString())
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.printer_settings_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.printer_back))
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshDevices) {
                        Icon(
                            Icons.Default.Refresh,
                            stringResource(R.string.printer_refresh),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        val statusMessage = when (val status = uiState.printerStatus) {
            is com.zatiaras.pos.feature.printer.domain.model.PrinterStatus.Disconnected ->
                stringResource(R.string.printer_status_disconnected)
            is com.zatiaras.pos.feature.printer.domain.model.PrinterStatus.Scanning ->
                stringResource(R.string.printer_status_scanning)
            is com.zatiaras.pos.feature.printer.domain.model.PrinterStatus.Connecting ->
                stringResource(R.string.printer_status_connecting, status.device.displayName)
            is com.zatiaras.pos.feature.printer.domain.model.PrinterStatus.Connected ->
                stringResource(R.string.printer_status_connected, status.device.displayName)
            is com.zatiaras.pos.feature.printer.domain.model.PrinterStatus.Printing ->
                stringResource(R.string.printer_status_printing_progress, status.progress)
            is com.zatiaras.pos.feature.printer.domain.model.PrinterStatus.PrintSuccess ->
                stringResource(R.string.printer_status_print_success)
            is com.zatiaras.pos.feature.printer.domain.model.PrinterStatus.Error ->
                status.message
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Put connection status first because every setting below depends
            // on knowing whether the target printer is ready.
            item {
                EnhancedConnectionStatusCard(
                    status = uiState.printerStatus,
                    statusMessage = statusMessage,
                    onDisconnect = onDisconnect,
                    onPrintTest = onPrintTest,
                )
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.printer_section_bluetooth_title),
                    subtitle = stringResource(R.string.printer_section_bluetooth_subtitle),
                )
            }

            // Auto-connect belongs near devices so users understand which
            // printer list it applies to.
            item {
                EnhancedToggleCard(
                    icon = Icons.Default.Bluetooth,
                    iconColor = IconColors.Bluetooth,
                    title = stringResource(R.string.printer_auto_connect_title),
                    subtitle = stringResource(R.string.printer_auto_connect_subtitle),
                    checked = uiState.autoConnect,
                    onCheckedChange = onAutoConnectChange,
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
                        onClick = { onConnectDevice(device) },
                    )
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.printer_section_print_title),
                    subtitle = stringResource(R.string.printer_section_print_subtitle),
                )
            }

            // Paper width affects both live print commands and the preview.
            item {
                EnhancedSettingsCard(
                    icon = Icons.Outlined.Print,
                    iconColor = SettingsIconColor,
                    title = stringResource(R.string.printer_paper_width_title),
                    content = {
                        Spacer(modifier = Modifier.height(12.dp))
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            SegmentedButton(
                                selected = uiState.paperWidth == PaperWidth.MM_58,
                                onClick = { onPaperWidthChange(PaperWidth.MM_58) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            ) {
                                Text(stringResource(R.string.printer_paper_58))
                            }
                            SegmentedButton(
                                selected = uiState.paperWidth == PaperWidth.MM_80,
                                onClick = { onPaperWidthChange(PaperWidth.MM_80) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            ) {
                                Text(stringResource(R.string.printer_paper_80))
                            }
                        }
                    },
                )
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.printer_section_store_title),
                    subtitle = stringResource(R.string.printer_section_store_subtitle),
                )
            }

            item {
                StoreLogoCard(
                    logoUri = uiState.storeLogoUri,
                    onSelectLogo = { imagePickerLauncher.launch("image/*") },
                    onClearLogo = onClearStoreLogo,
                )
            }

            item {
                EnhancedStoreInfoCard(
                    storeName = uiState.storeName,
                    storeAddress = uiState.storeAddress,
                    onStoreNameChange = onStoreNameChange,
                    onStoreAddressChange = onStoreAddressChange,
                    onSave = onSaveStoreInfo,
                )
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.printer_section_preview_title),
                    subtitle = stringResource(R.string.printer_section_preview_subtitle),
                )
            }

            item {
                ReceiptPreviewCard(
                    storeName = uiState.storeName,
                    storeAddress = uiState.storeAddress,
                    storeLogoUri = uiState.storeLogoUri,
                    paperWidth = uiState.paperWidth,
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun EnhancedToggleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.L),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.M)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
internal fun EnhancedSettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.L),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(AppShapes.M)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

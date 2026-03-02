package com.zatiaras.pos.feature.printer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.IconColors
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.SuccessGreen
import com.zatiaras.pos.feature.printer.domain.model.PrinterDevice
import com.zatiaras.pos.feature.printer.domain.model.PrinterStatus
import com.zatiaras.pos.feature.printer.R
import androidx.compose.foundation.border

private val PrinterIconColor = IconColors.Printer

@Composable
internal fun EnhancedConnectionStatusCard(
    status: PrinterStatus,
    statusMessage: String,
    onDisconnect: () -> Unit,
    onPrintTest: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val isConnected = status is PrinterStatus.Connected || 
                      status is PrinterStatus.Printing ||
                      status is PrinterStatus.PrintSuccess
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isConnected) SuccessGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                AppShapes.XL
            ),
        shape = AppShapes.XL,
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                SuccessGreen.copy(alpha = 0.05f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(dimensions.paddingL)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon with background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(AppShapes.L)
                        .background(
                            if (isConnected) SuccessGreen.copy(alpha = 0.2f)
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
                        tint = if (isConnected) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(dimensions.spacingM))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isConnected) stringResource(R.string.printer_connected_status) else stringResource(R.string.printer_disconnected_status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) SuccessGreen else MaterialTheme.colorScheme.onSurface
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
                Spacer(modifier = Modifier.height(dimensions.spacingL))
                HorizontalDivider(color = SuccessGreen.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(dimensions.spacingM))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.M
                    ) {
                        Text(stringResource(R.string.printer_disconnect))
                    }
                    
                    Button(
                        onClick = onPrintTest,
                        modifier = Modifier.weight(1f),
                        enabled = status !is PrinterStatus.Printing,
                        shape = AppShapes.M,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen
                        )
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.printer_test_print))
                    }
                }
            }
        }
    }
}

@Composable
internal fun EnhancedPrinterDeviceItem(
    device: PrinterDevice,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    val dimensions = LocalDimensions.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting && !device.isConnected, onClick = onClick)
            .border(
                1.dp,
                if (device.isConnected) SuccessGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                AppShapes.L
            ),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = if (device.isConnected) 
                SuccessGreen.copy(alpha = 0.05f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.M)
                    .background(
                        if (device.isConnected) SuccessGreen.copy(alpha = 0.15f)
                        else PrinterIconColor.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (device.isLikelyPrinter) Icons.Default.Print else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (device.isConnected) SuccessGreen else PrinterIconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(dimensions.spacingM))
            
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
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.printer_connected),
                            tint = MaterialTheme.colorScheme.onPrimary,
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
internal fun EnhancedEmptyDevicesCard(isBluetoothEnabled: Boolean) {
    val dimensions = LocalDimensions.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingXXL),
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
            Spacer(modifier = Modifier.height(dimensions.spacingM))
            Text(
                text = if (isBluetoothEnabled) 
                    stringResource(R.string.printer_no_devices_found)
                else 
                    stringResource(R.string.printer_bluetooth_disabled_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isBluetoothEnabled)
                    stringResource(R.string.printer_no_devices_hint)
                else
                    stringResource(R.string.printer_enable_bluetooth_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

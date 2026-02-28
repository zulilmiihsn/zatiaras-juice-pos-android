package com.zatiaras.pos.feature.printer.presentation

import com.zatiaras.pos.feature.printer.domain.model.PaperWidth
import com.zatiaras.pos.feature.printer.domain.model.PrinterDevice
import com.zatiaras.pos.feature.printer.domain.model.PrinterStatus

/**
 * UI State for Printer Settings screen.
 */
data class PrinterSettingsUiState(
    val isLoading: Boolean = false,
    val printerStatus: PrinterStatus = PrinterStatus.Disconnected,
    val pairedDevices: List<PrinterDevice> = emptyList(),
    val selectedDevice: PrinterDevice? = null,
    val paperWidth: PaperWidth = PaperWidth.MM_58,
    val storeName: String = "Zatiaras Juice",
    val storeAddress: String = "",
    val storeLogoUri: String? = null, // null = use default app logo
    val autoConnect: Boolean = false,
    val errorMessage: String? = null,
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothPermission: Boolean = false
) {
    val isConnected: Boolean
        get() = printerStatus is PrinterStatus.Connected
    
    val isConnecting: Boolean
        get() = printerStatus is PrinterStatus.Connecting
    
    val isPrinting: Boolean
        get() = printerStatus is PrinterStatus.Printing
    
    val connectedPrinter: PrinterDevice?
        get() = when (printerStatus) {
            is PrinterStatus.Connected -> printerStatus.device
            is PrinterStatus.Printing -> printerStatus.device
            is PrinterStatus.PrintSuccess -> printerStatus.device
            else -> null
        }
}

/**
 * Events from ViewModel to UI.
 */
sealed class PrinterEvent {
    data class ShowToast(val message: String) : PrinterEvent()
    data object RequestBluetoothEnable : PrinterEvent()
    data object RequestBluetoothPermissions : PrinterEvent()
    data object PrintComplete : PrinterEvent()
}

package com.zatiaras.pos.feature.printer.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.feature.printer.data.bluetooth.BluetoothPrinterManager
import com.zatiaras.pos.feature.printer.data.escpos.ReceiptFormatter
import com.zatiaras.pos.feature.printer.data.preferences.PrinterPreferences
import com.zatiaras.pos.feature.printer.domain.model.PaperWidth
import com.zatiaras.pos.feature.printer.domain.model.PrinterDevice
import com.zatiaras.pos.feature.printer.domain.model.PrinterStatus
import com.zatiaras.pos.feature.printer.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PrinterSettingsViewModel @Inject constructor(
    private val printerManager: BluetoothPrinterManager,
    private val printerPreferences: PrinterPreferences,
    private val receiptFormatter: ReceiptFormatter,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PrinterSettingsUiState())
    val uiState: StateFlow<PrinterSettingsUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<PrinterEvent>()
    val events: SharedFlow<PrinterEvent> = _events.asSharedFlow()
    
    init {
        loadSettings()
        observePrinterStatus()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val paperWidth = printerPreferences.getPaperWidth()
                val storeName = printerPreferences.getStoreName()
                val storeAddress = printerPreferences.getStoreAddress() ?: ""
                val storeLogo = printerPreferences.getStoreLogo()
                val autoConnect = printerPreferences.isAutoConnectEnabled()
                val lastPrinter = printerPreferences.getLastPrinter()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        paperWidth = paperWidth,
                        storeName = storeName,
                        storeAddress = storeAddress,
                        storeLogoUri = storeLogo,
                        autoConnect = autoConnect,
                        selectedDevice = lastPrinter,
                        isBluetoothEnabled = printerManager.isBluetoothEnabled()
                    )
                }
                
                // Auto-connect if enabled and has last printer
                if (autoConnect && lastPrinter != null) {
                    connectToPrinter(lastPrinter)
                }
                
                // Load paired devices
                refreshPairedDevices()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load printer settings")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private fun observePrinterStatus() {
        viewModelScope.launch {
            printerManager.status.collect { status ->
                _uiState.update { it.copy(printerStatus = status) }
                
                if (status is PrinterStatus.Error) {
                    _events.emit(PrinterEvent.ShowToast(status.message))
                }
            }
        }
    }
    
    fun refreshPairedDevices() {
        viewModelScope.launch {
            if (!printerManager.isBluetoothEnabled()) {
                _uiState.update { it.copy(isBluetoothEnabled = false) }
                _events.emit(PrinterEvent.RequestBluetoothEnable)
                return@launch
            }
            
            val devices = printerManager.getPairedDevices()
            _uiState.update {
                it.copy(
                    pairedDevices = devices,
                    isBluetoothEnabled = true
                )
            }
        }
    }
    
    fun connectToPrinter(device: PrinterDevice) {
        viewModelScope.launch {
            if (!printerManager.isBluetoothEnabled()) {
                _events.emit(PrinterEvent.RequestBluetoothEnable)
                return@launch
            }
            
            _uiState.update { it.copy(selectedDevice = device) }
            
            printerManager.connect(device).fold(
                onSuccess = {
                    printerPreferences.saveLastPrinter(device)
                    _events.emit(
                        PrinterEvent.ShowToast(
                            context.getString(R.string.printer_connected_to_device, device.displayName)
                        )
                    )
                    refreshPairedDevices()
                },
                onFailure = { error ->
                    _events.emit(
                        PrinterEvent.ShowToast(
                            context.getString(
                                R.string.printer_failed_with_reason,
                                error.message ?: context.getString(R.string.printer_error_unknown)
                            )
                        )
                    )
                }
            )
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            printerManager.disconnect()
            _events.emit(PrinterEvent.ShowToast(context.getString(R.string.printer_disconnected_toast)))
            refreshPairedDevices()
        }
    }
    
    fun printTestPage() {
        viewModelScope.launch {
            if (!printerManager.isConnected()) {
                _events.emit(PrinterEvent.ShowToast(context.getString(R.string.printer_not_connected)))
                return@launch
            }
            
            val paperWidth = printerPreferences.getPaperWidth()
            val testData = receiptFormatter.formatTestPage(paperWidth)
            
            printerManager.print(testData).fold(
                onSuccess = {
                    _events.emit(PrinterEvent.PrintComplete)
                },
                onFailure = { error ->
                    _events.emit(
                        PrinterEvent.ShowToast(
                            context.getString(
                                R.string.printer_print_failed_with_reason,
                                error.message ?: context.getString(R.string.printer_error_unknown)
                            )
                        )
                    )
                }
            )
        }
    }
    
    fun setPaperWidth(width: PaperWidth) {
        viewModelScope.launch {
            printerPreferences.savePaperWidth(width)
            _uiState.update { it.copy(paperWidth = width) }
        }
    }
    
    fun setStoreName(name: String) {
        _uiState.update { it.copy(storeName = name) }
    }
    
    fun setStoreAddress(address: String) {
        _uiState.update { it.copy(storeAddress = address) }
    }
    
    fun saveStoreInfo() {
        viewModelScope.launch {
            val state = _uiState.value
            printerPreferences.saveStoreInfo(
                name = state.storeName,
                address = state.storeAddress.ifBlank { null }
            )
            printerPreferences.saveStoreLogo(state.storeLogoUri)
            _events.emit(PrinterEvent.ShowToast(context.getString(R.string.printer_store_info_saved)))
        }
    }
    
    fun setStoreLogo(uri: String?) {
        _uiState.update { it.copy(storeLogoUri = uri) }
    }
    
    fun clearStoreLogo() {
        viewModelScope.launch {
            printerPreferences.clearStoreLogo()
            _uiState.update { it.copy(storeLogoUri = null) }
        }
    }
    
    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            printerPreferences.setAutoConnect(enabled)
            _uiState.update { it.copy(autoConnect = enabled) }
        }
    }
    
    fun onBluetoothPermissionGranted() {
        _uiState.update { it.copy(hasBluetoothPermission = true) }
        refreshPairedDevices()
    }
    
    fun onBluetoothEnabled() {
        _uiState.update { it.copy(isBluetoothEnabled = true) }
        refreshPairedDevices()
    }
}

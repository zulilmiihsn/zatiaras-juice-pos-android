package com.zatiaras.pos.feature.printer.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.zatiaras.pos.feature.printer.R
import com.zatiaras.pos.feature.printer.domain.model.PrinterDevice
import com.zatiaras.pos.feature.printer.domain.model.PrinterStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for Bluetooth printer operations.
 * 
 * Handles:
 * - Discovering paired Bluetooth devices
 * - Connecting to thermal printers
 * - Sending print data
 * - Connection state management
 */
@Singleton
class BluetoothPrinterManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Standard Serial Port Profile UUID for Bluetooth printers
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        // Connection timeout in milliseconds
        private const val CONNECT_TIMEOUT_MS = 10_000L
    }
    
    private val bluetoothManager: BluetoothManager? = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    
    private var currentSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var connectedDevice: PrinterDevice? = null
    
    private val _status = MutableStateFlow<PrinterStatus>(PrinterStatus.Disconnected)
    val status: StateFlow<PrinterStatus> = _status.asStateFlow()
    
    /**
     * Check if Bluetooth is available on this device.
     */
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null
    
    /**
     * Check if Bluetooth is enabled.
     */
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    
    /**
     * Get list of paired Bluetooth devices.
     * Returns empty list if Bluetooth is not available or enabled.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<PrinterDevice> {
        if (!isBluetoothEnabled()) {
            Timber.w("Bluetooth not enabled, cannot get paired devices")
            return emptyList()
        }
        
        return try {
            bluetoothAdapter?.bondedDevices?.map { device ->
                PrinterDevice(
                    name = device.name ?: "",
                    address = device.address,
                    isPaired = true,
                    isConnected = device.address == connectedDevice?.address
                )
            } ?: emptyList()
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied for getting paired devices")
            emptyList()
        }
    }
    
    /**
     * Get only devices that look like printers based on name.
     */
    fun getPairedPrinters(): List<PrinterDevice> {
        return getPairedDevices().filter { it.isLikelyPrinter }
    }
    
    /**
     * Connect to a Bluetooth printer.
     * 
     * @param device The printer device to connect to
     * @return Result with Unit on success, error on failure
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(device: PrinterDevice): Result<Unit> = withContext(Dispatchers.IO) {
        // Disconnect existing connection first
        disconnect()
        
        _status.value = PrinterStatus.Connecting(device)
        Timber.d("Connecting to printer: ${device.displayName} (${device.address})")
        
        try {
            val bluetoothDevice: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                ?: return@withContext Result.failure(Exception(context.getString(R.string.printer_bluetooth_adapter_unavailable)))
            
            // Create socket
            val socket = bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID)
            
            // Cancel discovery to speed up connection
            try {
                bluetoothAdapter.cancelDiscovery()
            } catch (e: SecurityException) {
                Timber.w("Could not cancel discovery: ${e.message}")
            }
            
            // Connect with timeout
            try {
                socket.connect()
            } catch (e: IOException) {
                Timber.e(e, "Failed to connect to printer")
                _status.value = PrinterStatus.Error(
                    message = context.getString(
                        R.string.printer_connect_failed_with_reason,
                        e.message ?: context.getString(R.string.printer_error_unknown)
                    ),
                    isRecoverable = true
                )
                return@withContext Result.failure(e)
            }
            
            // Store connection
            currentSocket = socket
            outputStream = socket.outputStream
            connectedDevice = device.copy(isConnected = true)
            
            _status.value = PrinterStatus.Connected(connectedDevice!!)
            Timber.d("Connected to printer: ${device.displayName}")
            
            Result.success(Unit)
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied for Bluetooth connection")
            _status.value = PrinterStatus.Error(
                message = context.getString(R.string.printer_bluetooth_permission_denied),
                isRecoverable = true
            )
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error connecting to printer")
            _status.value = PrinterStatus.Error(
                message = context.getString(
                    R.string.printer_error_with_reason,
                    e.message ?: context.getString(R.string.printer_error_unknown)
                ),
                isRecoverable = true
            )
            Result.failure(e)
        }
    }
    
    /**
     * Disconnect from current printer.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            outputStream?.close()
            currentSocket?.close()
        } catch (e: IOException) {
            Timber.w(e, "Error closing Bluetooth connection")
        } finally {
            outputStream = null
            currentSocket = null
            connectedDevice = null
            _status.value = PrinterStatus.Disconnected
            Timber.d("Disconnected from printer")
        }
    }
    
    /**
     * Check if currently connected to a printer.
     */
    fun isConnected(): Boolean = currentSocket?.isConnected == true
    
    /**
     * Get the currently connected device.
     */
    fun getConnectedDevice(): PrinterDevice? = connectedDevice
    
    /**
     * Send raw bytes to the printer.
     * 
     * @param data The ESC/POS formatted byte data
     * @return Result with Unit on success, error on failure
     */
    suspend fun print(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val device = connectedDevice
        if (device == null || outputStream == null) {
            val error = context.getString(R.string.printer_not_connected)
            _status.value = PrinterStatus.Error(message = error, isRecoverable = true)
            return@withContext Result.failure(Exception(error))
        }
        
        _status.value = PrinterStatus.Printing(device, 0)
        Timber.d("Printing ${data.size} bytes to ${device.displayName}")
        
        try {
            // Send data in chunks to avoid buffer overflow
            val chunkSize = 1024
            var bytesSent = 0
            
            for (i in data.indices step chunkSize) {
                val end = minOf(i + chunkSize, data.size)
                val chunk = data.copyOfRange(i, end)
                
                outputStream?.write(chunk)
                outputStream?.flush()
                
                bytesSent += chunk.size
                val progress = (bytesSent * 100) / data.size
                _status.value = PrinterStatus.Printing(device, progress)
                
                // Small delay between chunks
                delay(50)
            }
            
            _status.value = PrinterStatus.PrintSuccess(device)
            Timber.d("Print completed: $bytesSent bytes sent")
            
            // Reset to connected status after a short delay
            delay(2000)
            _status.value = PrinterStatus.Connected(device)
            
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "Print failed")
            _status.value = PrinterStatus.Error(
                message = context.getString(
                    R.string.printer_print_failed_with_reason,
                    e.message ?: context.getString(R.string.printer_error_unknown)
                ),
                isRecoverable = true
            )
            
            // Connection might be broken, try to reconnect on next print
            disconnect()
            
            Result.failure(e)
        }
    }
    
    /**
     * Print a test page to verify printer is working.
     */
    suspend fun printTestPage(testData: ByteArray): Result<Unit> {
        return print(testData)
    }
}

package com.zatiaras.pos.feature.printer.domain.model

/**
 * Represents the current status of printer connection and operations.
 */
sealed class PrinterStatus {
    
    /**
     * No printer connected.
     */
    data object Disconnected : PrinterStatus()
    
    /**
     * Currently searching for printers.
     */
    data object Scanning : PrinterStatus()
    
    /**
     * Attempting to connect to a printer.
     */
    data class Connecting(val device: PrinterDevice) : PrinterStatus()
    
    /**
     * Successfully connected to a printer.
     */
    data class Connected(val device: PrinterDevice) : PrinterStatus()
    
    /**
     * Currently printing a receipt.
     */
    data class Printing(val device: PrinterDevice, val progress: Int = 0) : PrinterStatus()
    
    /**
     * Print completed successfully.
     */
    data class PrintSuccess(val device: PrinterDevice) : PrinterStatus()
    
    /**
     * An error occurred.
     */
    data class Error(
        val message: String,
        val isRecoverable: Boolean = true
    ) : PrinterStatus()
}

/**
 * Paper width for thermal printers.
 */
enum class PaperWidth(val mmWidth: Int, val charPerLine: Int) {
    MM_58(58, 32),   // 58mm paper - 32 chars per line
    MM_80(80, 48)    // 80mm paper - 48 chars per line
}

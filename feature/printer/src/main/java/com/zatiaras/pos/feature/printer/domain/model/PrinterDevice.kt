package com.zatiaras.pos.feature.printer.domain.model

/**
 * Represents a Bluetooth printer device.
 */
data class PrinterDevice(
    val name: String,
    val address: String,  // MAC address
    val isPaired: Boolean = false,
    val isConnected: Boolean = false,
    val rssi: Int? = null  // Signal strength (optional)
) {
    /**
     * Display name for UI, falls back to address if name is empty.
     */
    val displayName: String
        get() = name.ifBlank { address }
    
    /**
     * Check if this is likely a thermal printer based on name.
     */
    val isLikelyPrinter: Boolean
        get() {
            val lowerName = name.lowercase()
            return lowerName.contains("printer") ||
                    lowerName.contains("pos") ||
                    lowerName.contains("thermal") ||
                    lowerName.contains("epson") ||
                    lowerName.contains("xprinter") ||
                    lowerName.contains("goojprt") ||
                    lowerName.contains("bluetooth printer") ||
                    lowerName.contains("mpt") ||
                    lowerName.contains("pt-") ||
                    lowerName.contains("rpp")
        }
}

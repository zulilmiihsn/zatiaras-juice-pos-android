package com.zatiaras.pos.feature.printer.data.escpos

/**
 * ESC/POS command constants for thermal printers.
 * 
 * These are standard commands that work with most thermal printers
 * including EPSON, Xprinter, GOOJPRT, and similar brands.
 */
object EscPosCommands {
    
    // ==================== INITIALIZATION ====================
    
    /**
     * Initialize printer - resets settings to default.
     * ESC @
     */
    val INIT = byteArrayOf(0x1B, 0x40)
    
    // ==================== TEXT FORMATTING ====================
    
    /**
     * Line feed (new line).
     * LF
     */
    val LINE_FEED = byteArrayOf(0x0A)
    
    /**
     * Carriage return.
     * CR
     */
    val CARRIAGE_RETURN = byteArrayOf(0x0D)
    
    /**
     * Bold ON.
     * ESC E 1
     */
    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    
    /**
     * Bold OFF.
     * ESC E 0
     */
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    
    /**
     * Underline ON.
     * ESC - 1
     */
    val UNDERLINE_ON = byteArrayOf(0x1B, 0x2D, 0x01)
    
    /**
     * Underline OFF.
     * ESC - 0
     */
    val UNDERLINE_OFF = byteArrayOf(0x1B, 0x2D, 0x00)
    
    /**
     * Double height ON.
     * GS ! 0x10
     */
    val DOUBLE_HEIGHT_ON = byteArrayOf(0x1D, 0x21, 0x10)
    
    /**
     * Double width ON.
     * GS ! 0x20
     */
    val DOUBLE_WIDTH_ON = byteArrayOf(0x1D, 0x21, 0x20)
    
    /**
     * Double height and width ON (2x size).
     * GS ! 0x30
     */
    val DOUBLE_SIZE_ON = byteArrayOf(0x1D, 0x21, 0x30)
    
    /**
     * Normal size (reset height/width).
     * GS ! 0
     */
    val NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00)
    
    // ==================== TEXT ALIGNMENT ====================
    
    /**
     * Align LEFT.
     * ESC a 0
     */
    val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    
    /**
     * Align CENTER.
     * ESC a 1
     */
    val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    
    /**
     * Align RIGHT.
     * ESC a 2
     */
    val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    
    // ==================== PAPER CUTTING ====================
    
    /**
     * Full cut (if supported).
     * GS V 0
     */
    val CUT_FULL = byteArrayOf(0x1D, 0x56, 0x00)
    
    /**
     * Partial cut (if supported).
     * GS V 1
     */
    val CUT_PARTIAL = byteArrayOf(0x1D, 0x56, 0x01)
    
    /**
     * Feed and cut - feeds 3 lines then cuts.
     * GS V 66 3
     */
    val FEED_AND_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x03)
    
    // ==================== PAPER FEED ====================
    
    /**
     * Feed n lines.
     * ESC d n
     */
    fun feedLines(n: Int): ByteArray = byteArrayOf(0x1B, 0x64, n.toByte())
    
    /**
     * Feed paper 3 lines (commonly used before cut).
     */
    val FEED_3_LINES = feedLines(3)
    
    /**
     * Feed paper 5 lines.
     */
    val FEED_5_LINES = feedLines(5)
    
    // ==================== BARCODE ====================
    
    /**
     * Set barcode height (in dots).
     * GS h n
     */
    fun setBarcodeHeight(height: Int): ByteArray = byteArrayOf(0x1D, 0x68, height.toByte())
    
    /**
     * Set barcode width multiplier (2-6).
     * GS w n
     */
    fun setBarcodeWidth(width: Int): ByteArray = byteArrayOf(0x1D, 0x77, width.coerceIn(2, 6).toByte())
    
    /**
     * Print barcode (CODE128).
     * GS k 73 length data...
     */
    fun printBarcode(data: String): ByteArray {
        val bytes = data.toByteArray(Charsets.US_ASCII)
        return byteArrayOf(0x1D, 0x6B, 0x49, bytes.size.toByte()) + bytes
    }
    
    // ==================== QR CODE ====================
    
    /**
     * Print QR code.
     * This is a multi-step command sequence.
     */
    fun printQrCode(data: String, size: Int = 6): ByteArray {
        val bytes = data.toByteArray(Charsets.UTF_8)
        val storeLen = bytes.size + 3
        val pL = storeLen and 0xFF
        val pH = (storeLen shr 8) and 0xFF
        
        return byteArrayOf(
            // Set QR code model
            0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00,
            // Set QR code size
            0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.toByte(),
            // Set error correction level (L)
            0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30
        ) + byteArrayOf(
            // Store QR data
            0x1D, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30
        ) + bytes + byteArrayOf(
            // Print QR code
            0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30
        )
    }
    
    // ==================== HELPER FUNCTIONS ====================
    
    /**
     * Convert text to bytes with charset handling.
     */
    fun text(content: String): ByteArray = content.toByteArray(Charsets.UTF_8)
    
    /**
     * Create a separator line (dashes).
     */
    fun separator(charCount: Int = 32): ByteArray = 
        "-".repeat(charCount).toByteArray(Charsets.UTF_8) + LINE_FEED
    
    /**
     * Create a double separator line (equals).
     */
    fun doubleSeparator(charCount: Int = 32): ByteArray =
        "=".repeat(charCount).toByteArray(Charsets.UTF_8) + LINE_FEED
}

package com.zatiaras.pos.core.data.access

/**
 * User roles in the ZatiarasPOS system.
 * 
 * Role hierarchy:
 * - PEMILIK (Owner): Full access to all features
 * - KASIR (Cashier): Limited access, some screens require owner PIN
 */
enum class UserRole(val value: String) {
    PEMILIK("pemilik"),
    KASIR("kasir");

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value?.lowercase()) {
                "pemilik" -> PEMILIK
                "kasir" -> KASIR
                else -> KASIR // Default to kasir for safety
            }
        }
    }

    /**
     * Check if this role has full access (owner privileges).
     */
    fun isOwner(): Boolean = this == PEMILIK

    /**
     * Check if this role is restricted (cashier).
     */
    fun isRestricted(): Boolean = this == KASIR
}

/**
 * Lockable routes that can be protected by owner PIN.
 * 
 * When a kasir tries to access a locked route, they must enter the owner PIN.
 */
enum class LockableRoute(val route: String, val displayName: String) {
    // Full screen routes
    SETTINGS("settings", "Pengaturan"),
    INVENTORY("inventory", "Inventaris"),
    PNL_REPORT("pnl_report", "Laporan Laba Rugi"),
    PRINTER_SETTINGS("printer_settings", "Pengaturan Printer"),
    
    // Tab routes
    REPORTS_TAB("reports", "Tab Laporan"),
    CASH_RECORD_TAB("cash_record", "Tab Buku Kas");

    companion object {
        fun fromRoute(route: String): LockableRoute? {
            return entries.find { it.route == route }
        }
        
        /**
         * Get all lockable routes as a list.
         */
        fun all(): List<LockableRoute> = entries.toList()
    }
}

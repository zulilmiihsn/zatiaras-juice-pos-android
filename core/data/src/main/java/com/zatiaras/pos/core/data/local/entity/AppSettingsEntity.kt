package com.zatiaras.pos.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Room Entity for Application Settings.
 * Maps to Supabase table: "pengaturan"
 * 
 * This is a singleton table (only 1 row) that stores:
 * - Owner PIN for access control
 * - Locked routes that kasir cannot access without PIN
 * - Store info for receipt printing
 * - Default printer paper width
 * 
 * All settings sync across devices via Supabase.
 */
@Entity(tableName = "app_settings")
@TypeConverters(LockedRoutesConverter::class)
data class AppSettingsEntity(
    @PrimaryKey
    val id: String = "default", // Singleton - always "default" or branch ID
    
    // Access Control
    val ownerPinHash: String? = null,
    val lockedRoutes: List<String> = emptyList(),
    
    // Store Info (for receipt)
    val storeName: String = "Zatiaras Juice",
    val storeAddress: String? = null,
    val storePhone: String? = null,
    
    // Printer defaults
    val defaultPaperWidth: Int = 58, // 58 or 80 mm
    
    // Receipt customization
    val receiptFooter: String? = "Terima kasih atas kunjungan Anda!",
    val showLogoOnReceipt: Boolean = true,
    
    // Tax defaults
    val defaultTaxPercentage: Double = 0.5,
    
    // Performance
    val lowPerformanceMode: Boolean = false,
    
    // Sync tracking
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

/**
 * Type converter for List<String> to store locked routes.
 * Uses comma-separated format for simplicity.
 */
class LockedRoutesConverter {
    @TypeConverter
    fun fromList(routes: List<String>): String {
        return routes.joinToString(",")
    }
    
    @TypeConverter
    fun toList(routesString: String): List<String> {
        if (routesString.isBlank()) return emptyList()
        return routesString.split(",").filter { it.isNotBlank() }
    }
}

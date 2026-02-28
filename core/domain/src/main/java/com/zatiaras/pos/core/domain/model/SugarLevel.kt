package com.zatiaras.pos.core.domain.model

/**
 * Sugar level options for beverages.
 * 
 * Matches web app values exactly:
 * - NO: "no" -> "Tanpa Gula"
 * - LESS: "less" -> "Sedikit Gula"
 * - NORMAL: "normal" -> "Normal"
 */
enum class SugarLevel(val id: String, val label: String) {
    NO("no", "Tanpa Gula"),
    LESS("less", "Sedikit Gula"),
    NORMAL("normal", "Normal");
    
    companion object {
        fun fromId(id: String?): SugarLevel {
            return entries.find { it.id == id?.lowercase() } ?: NORMAL
        }
    }
}

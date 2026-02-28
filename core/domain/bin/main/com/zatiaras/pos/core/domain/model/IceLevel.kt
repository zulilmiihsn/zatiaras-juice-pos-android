package com.zatiaras.pos.core.domain.model

/**
 * Ice level options for beverages.
 * 
 * Matches web app values exactly:
 * - NO: "no" -> "Tanpa Es"
 * - LESS: "less" -> "Sedikit Es"
 * - NORMAL: "normal" -> "Normal"
 */
enum class IceLevel(val id: String, val label: String) {
    NO("no", "Tanpa Es"),
    LESS("less", "Sedikit Es"),
    NORMAL("normal", "Normal");
    
    companion object {
        fun fromId(id: String?): IceLevel {
            return entries.find { it.id == id?.lowercase() } ?: NORMAL
        }
    }
}

package com.zatiaras.pos.core.domain.model

data class StoreSession(
    val id: String,
    val openingCash: Long,
    val openingTime: Long,
    val closingTime: Long? = null,
    val isActive: Boolean,
    val branchId: String? = null
)

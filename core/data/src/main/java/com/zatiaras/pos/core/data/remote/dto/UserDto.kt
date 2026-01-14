package com.zatiaras.pos.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for user data from Supabase.
 * Maps to the 'pengguna' table in Supabase.
 */
@Serializable
data class UserDto(
    @SerialName("id")
    val id: String,
    
    @SerialName("username")
    val username: String,
    
    @SerialName("password_hash")
    val passwordHash: String,
    
    @SerialName("display_name")
    val displayName: String,
    
    @SerialName("role")
    val role: String,
    
    @SerialName("is_active")
    val isActive: Boolean = true,
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)

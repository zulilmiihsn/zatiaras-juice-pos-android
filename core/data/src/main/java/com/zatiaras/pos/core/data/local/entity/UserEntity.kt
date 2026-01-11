package com.zatiaras.pos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest

/**
 * Local user entity for offline authentication.
 * 
 * Stores user credentials locally in Room database,
 * allowing login without internet connection.
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val passwordHash: String,
    val displayName: String,
    val role: String = "kasir", // kasir, pemilik
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    companion object {
        /**
         * Hash password using SHA-256.
         * For production, consider using bcrypt or Argon2.
         */
        fun hashPassword(password: String): String {
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest(password.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
        
        /**
         * Verify password against stored hash.
         */
        fun verifyPassword(password: String, hash: String): Boolean {
            return hashPassword(password) == hash
        }
    }
}

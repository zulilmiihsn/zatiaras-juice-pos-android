package com.zatiaras.pos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zatiaras.pos.core.data.util.PasswordHasher

/**
 * Local user entity for offline authentication.
 * 
 * Stores user credentials locally in Room database,
 * allowing login without internet connection.
 * 
 * Password Security:
 * - Uses PBKDF2WithHmacSHA256 with 120,000 iterations
 * - Random salt per password
 * - Backward compatible with legacy SHA-256 hashes
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
         * Hash password using PBKDF2WithHmacSHA256.
         * Returns format: "salt:hash"
         */
        fun hashPassword(password: String): String {
            return PasswordHasher.hash(password)
        }
        
        /**
         * Verify password against stored hash.
         * Supports both PBKDF2 and legacy SHA-256 formats.
         */
        fun verifyPassword(password: String, hash: String): Boolean {
            return PasswordHasher.verify(password, hash)
        }
        
        /**
         * Check if password hash needs upgrading to new format.
         */
        fun needsRehash(hash: String): Boolean {
            return PasswordHasher.needsRehash(hash)
        }
    }
}

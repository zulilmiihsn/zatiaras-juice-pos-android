package com.zatiaras.pos.core.data.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Secure password hashing utility using PBKDF2WithHmacSHA256.
 * 
 * Features:
 * - Industry-standard PBKDF2 with 120,000 iterations
 * - Random salt for each password (prevents rainbow table attacks)
 * - Backward compatibility with legacy SHA-256 hashes
 * 
 * Hash format: "salt_hex:hash_hex" (e.g., "a1b2c3...:d4e5f6...")
 * Legacy format: "hash_hex" (no colon, pure SHA-256)
 */
object PasswordHasher {
    
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    
    /**
     * Hash a password using PBKDF2WithHmacSHA256.
     * 
     * @param password Plain text password
     * @return Hashed password in format "salt:hash"
     */
    fun hash(password: String): String {
        val salt = generateSalt()
        val hash = pbkdf2(password, salt)
        return "${salt.toHex()}:${hash.toHex()}"
    }

    /**
     * Hash a PIN using the same PBKDF2 scheme as password hashing.
     */
    fun hashPin(pin: String): String = hash(pin)
    
    /**
     * Verify a password against a stored hash.
     * Supports both new PBKDF2 format and legacy SHA-256 format.
     * 
     * @param password Plain text password to verify
     * @param storedHash Stored hash (either "salt:hash" or legacy SHA-256)
     * @return true if password matches
     */
    fun verify(password: String, storedHash: String): Boolean {
        return if (isLegacyHash(storedHash)) {
            verifyLegacy(password, storedHash)
        } else {
            verifyPbkdf2(password, storedHash)
        }
    }

    /**
     * Verify PIN against stored hash (PBKDF2 or legacy SHA-256).
     */
    fun verifyPin(pin: String, storedHash: String): Boolean = verify(pin, storedHash)
    
    /**
     * Check if a hash is in legacy SHA-256 format.
     * Legacy hashes don't contain a colon separator.
     */
    fun isLegacyHash(hash: String): Boolean = !hash.contains(":")
    
    /**
     * Check if a password needs rehashing (is using legacy format).
     * Use this to prompt users to update their passwords.
     */
    fun needsRehash(storedHash: String): Boolean = isLegacyHash(storedHash)
    
    // ==================== Private Methods ====================
    
    private fun generateSalt(): ByteArray {
        return ByteArray(SALT_LENGTH).also { 
            SecureRandom().nextBytes(it) 
        }
    }
    
    private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
    
    private fun verifyPbkdf2(password: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 2) return false
            
            val salt = parts[0].hexToBytes()
            val expectedHash = parts[1]
            val actualHash = pbkdf2(password, salt).toHex()
            
            // Constant-time comparison to prevent timing attacks
            constantTimeEquals(expectedHash, actualHash)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun verifyLegacy(password: String, storedHash: String): Boolean {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
        return constantTimeEquals(bytes.toHex(), storedHash)
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
    
    // ==================== Extension Functions ====================
    
    private fun ByteArray.toHex(): String = 
        joinToString("") { "%02x".format(it) }
    
    private fun String.hexToBytes(): ByteArray {
        check(length % 2 == 0) { "Hex string must have even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}

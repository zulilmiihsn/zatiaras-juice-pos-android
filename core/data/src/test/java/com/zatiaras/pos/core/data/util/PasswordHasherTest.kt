package com.zatiaras.pos.core.data.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PasswordHasherTest {

    @Test
    fun `hash should produce different values for same input due to random salt`() {
        val first = PasswordHasher.hash("secret123")
        val second = PasswordHasher.hash("secret123")

        assertNotEquals(first, second)
        assertTrue(PasswordHasher.verify("secret123", first))
        assertTrue(PasswordHasher.verify("secret123", second))
    }

    @Test
    fun `verify should return true for correct password and false for wrong password`() {
        val hash = PasswordHasher.hash("admin123")

        assertTrue(PasswordHasher.verify("admin123", hash))
        assertFalse(PasswordHasher.verify("wrong", hash))
    }

    @Test
    fun `verify should support legacy sha256 hash format`() {
        val legacyHash = sha256("1234")

        assertTrue(PasswordHasher.verifyPin("1234", legacyHash))
        assertFalse(PasswordHasher.verifyPin("9999", legacyHash))
        assertTrue(PasswordHasher.needsRehash(legacyHash))
    }

    @Test
    fun `verifyPin should work with pbkdf2 pin hash`() {
        val pinHash = PasswordHasher.hashPin("5678")

        assertTrue(PasswordHasher.verifyPin("5678", pinHash))
        assertFalse(PasswordHasher.verifyPin("0000", pinHash))
        assertFalse(PasswordHasher.needsRehash(pinHash))
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

package com.arverse.arbattery.core.network.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoTokenManagerTest {

    @Test
    fun `generateSecureToken produces unique non-empty tokens`() {
        val token1 = CryptoTokenManager.generateSecureToken()
        val token2 = CryptoTokenManager.generateSecureToken()

        assertTrue(token1.isNotBlank())
        assertTrue(token2.isNotBlank())
        assertNotEquals(token1, token2)
    }

    @Test
    fun `hashToken produces consistent SHA-256 output`() {
        val token = "test-token-secret-123"
        val hash1 = CryptoTokenManager.hashToken(token)
        val hash2 = CryptoTokenManager.hashToken(token)

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // 256 bits = 64 hex characters
    }

    @Test
    fun `verifyToken returns true for matching token and false for invalid`() {
        val token = "companion-secret-token"
        val expectedHash = CryptoTokenManager.hashToken(token)

        assertTrue(CryptoTokenManager.verifyToken(token, expectedHash))
        assertFalse(CryptoTokenManager.verifyToken("wrong-token", expectedHash))
    }

    @Test
    fun `isTimestampFresh validates within threshold and rejects expired`() {
        val now = System.currentTimeMillis()
        assertTrue(CryptoTokenManager.isTimestampFresh(now))
        assertTrue(CryptoTokenManager.isTimestampFresh(now - 60_000L)) // 1 min ago
        assertFalse(CryptoTokenManager.isTimestampFresh(now - 10 * 60_000L)) // 10 mins ago (threshold is 5 mins)
    }
}

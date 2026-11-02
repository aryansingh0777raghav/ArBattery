package com.arverse.arbattery.core.network.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object CryptoTokenManager {

    private val secureRandom = SecureRandom()

    fun generateSecureToken(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyToken(providedToken: String, expectedHash: String): Boolean {
        val providedHash = hashToken(providedToken)
        return MessageDigest.isEqual(
            providedHash.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun isTimestampFresh(timestamp: Long, maxAgeMs: Long = 5 * 60 * 1000L): Boolean {
        val now = System.currentTimeMillis()
        val diff = Math.abs(now - timestamp)
        return diff <= maxAgeMs
    }
}

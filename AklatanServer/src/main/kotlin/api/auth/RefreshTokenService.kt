package com.hexhyperion.api.auth

import com.hexhyperion.utility.Hasher
import io.ktor.server.config.*
import java.security.SecureRandom
import java.util.*
import kotlin.time.Instant

class RefreshTokenService(val refreshTokenRepository: RefreshTokenRepository, private val config: ApplicationConfig) {
    suspend fun generate(userId: Int): String {
        val token = generateRandomString()
        val tokenHash = Hasher.sha256Hash(token)
        val expiryDays = config.property("jwt.refreshTokenTimeoutDays").getString()
        val expirationTime = Instant.fromEpochMilliseconds(
            System.currentTimeMillis() + expiryDays.toLong() * 24L * 60L * 60L * 1000L
        )
        refreshTokenRepository.save(userId, tokenHash, expirationTime)
        return token
    }

    suspend fun getUserId(token: String): Int? {
        val refreshToken = refreshTokenRepository.find(token) ?: return null
        return refreshToken.userId
    }

    private fun generateRandomString(): String {
        val secureRandom = SecureRandom()
        val tokenLength = config.property("jwt.refreshTokenLength").getString().toInt()
        val bytes = ByteArray(tokenLength)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    suspend fun validate(token: String): Boolean {
        val refreshToken = refreshTokenRepository.find(token)
        return refreshToken != null && refreshToken.expiresAt > Instant.fromEpochMilliseconds(System.currentTimeMillis())
    }

    suspend fun revoke(refreshToken: String) {
        refreshTokenRepository.delete(refreshToken)
    }

    suspend fun revokeAllForUser(userId: Int) {
        refreshTokenRepository.deleteAllForUser(userId)
    }
}
package com.hexhyperion.aklatan.api.auth.tokens

import com.hexhyperion.aklatan.utility.Hasher
import io.ktor.server.config.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class RefreshTokenService(val refreshTokenRepository: RefreshTokenRepository, private val config: ApplicationConfig) {
    suspend fun generate(userId: Int): String {
        val tokenLength = config.property("jwt.refreshTokenLength").getString().toInt()
        val token = Hasher.generateRandomString(tokenLength)
        val tokenHash = Hasher.sha256Hash(token)
        val expiryDays = config.property("jwt.refreshTokenTimeoutDays").getString().toInt()
        val expirationTime = Clock.System.now() + expiryDays.days
        refreshTokenRepository.save(userId, tokenHash, expirationTime)
        return token
    }

    suspend fun getUserId(token: String): Int? {
        val refreshToken = refreshTokenRepository.find(token) ?: return null
        return refreshToken.userId
    }

    suspend fun validate(token: String): Boolean {
        val refreshToken = refreshTokenRepository.find(token)
        return refreshToken != null && refreshToken.expiresAt > Clock.System.now()
    }

    suspend fun cleanupExpired() {
        refreshTokenRepository.deleteAllExpired()
    }

    suspend fun revoke(refreshToken: String) {
        refreshTokenRepository.delete(refreshToken)
    }

    suspend fun revokeAllForUser(userId: Int) {
        refreshTokenRepository.deleteAllForUser(userId)
    }
}
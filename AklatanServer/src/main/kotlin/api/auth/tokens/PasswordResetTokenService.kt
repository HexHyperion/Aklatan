package com.hexhyperion.aklatan.api.auth.tokens

import com.hexhyperion.aklatan.utility.Hasher
import io.ktor.server.config.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class PasswordResetTokenService(
    val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val config: ApplicationConfig
) {
    suspend fun generate(userId: Int): String {
        val tokenLength = config.property("auth.passwordResetTokenLength").getString().toInt()
        val token = Hasher.generateRandomString(tokenLength)
        val tokenHash = Hasher.sha256Hash(token)
        val expiryHours = config.property("auth.passwordResetTokenTimeoutHours").getString().toInt()
        val expirationTime = Clock.System.now() + expiryHours.hours
        passwordResetTokenRepository.save(userId, tokenHash, expirationTime)
        return token
    }

    suspend fun getUserIdOrNull(token: String): Int? {
        val passwordResetToken = passwordResetTokenRepository.find(token) ?: return null
        return passwordResetToken.userId
    }

    suspend fun validate(token: String): Boolean {
        val passwordResetToken = passwordResetTokenRepository.find(token)
        return passwordResetToken != null && passwordResetToken.expiresAt > Clock.System.now()
    }

    suspend fun cleanupExpired() {
        passwordResetTokenRepository.deleteAllExpired()
    }

    suspend fun revokeAllForUser(userId: Int) {
        passwordResetTokenRepository.deleteAllForUser(userId)
    }
}
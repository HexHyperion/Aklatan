package com.hexhyperion.aklatan.api.auth.tokens

import com.hexhyperion.aklatan.utility.Hasher
import io.ktor.server.config.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class RegistrationTokenService(
    val registrationTokenRepository: RegistrationTokenRepository,
    private val config: ApplicationConfig
) {
    suspend fun generate(userId: Int): String {
        val tokenLength = config.property("auth.registrationTokenLength").getString().toInt()
        val token = Hasher.generateRandomString(tokenLength)
        val tokenHash = Hasher.sha256Hash(token)
        val expiryHours = config.property("auth.registrationTokenTimeoutHours").getString().toInt()
        val expirationTime = Clock.System.now() + expiryHours.hours
        registrationTokenRepository.save(userId, tokenHash, expirationTime)
        return token
    }

    suspend fun getUserIdOrNull(token: String): Int? {
        val registrationToken = registrationTokenRepository.find(token) ?: return null
        return registrationToken.userId
    }

    suspend fun validate(token: String): Boolean {
        val registrationToken = registrationTokenRepository.find(token)
        return registrationToken != null && registrationToken.expiresAt > Clock.System.now()
    }

    suspend fun cleanupExpired() {
        registrationTokenRepository.deleteAllExpired()
    }

    suspend fun revokeAllForUser(userId: Int) {
        registrationTokenRepository.deleteAllForUser(userId)
    }
}
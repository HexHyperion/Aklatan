package com.hexhyperion.aklatan.api.auth.tokens

import com.hexhyperion.aklatan.db.*
import com.hexhyperion.aklatan.utility.Hasher
import org.jetbrains.exposed.v1.core.eq
import kotlin.time.Instant

class RegistrationTokenRepository {
    suspend fun save(userId: Int, tokenHash: String, expiresAt: Instant) = withTransaction {
        RegistrationTokenEntity.new {
            this.user = UserEntity.findById(userId) ?: throw IllegalArgumentException("User not found")
            this.tokenHash = tokenHash
            this.expiresAt = expiresAt
        }
    }

    suspend fun find(registrationToken: String): RegistrationToken? = withTransaction {
        RegistrationTokenEntity
            .find { RegistrationTokens.tokenHash eq Hasher.sha256Hash(registrationToken) }
            .firstOrNull()
            ?.toRegistrationToken()
    }

    suspend fun findAllForUser(userId: Int): List<RegistrationToken> = withTransaction {
        RegistrationTokenEntity
            .find { RegistrationTokens.user eq userId }
            .map { it.toRegistrationToken() }
    }

    suspend fun delete(registrationToken: String) = withTransaction {
        RegistrationTokenEntity.find { RegistrationTokens.tokenHash eq Hasher.sha256Hash(registrationToken) }
            .forEach { it.delete() }
    }

    suspend fun deleteAllForUser(userId: Int) = withTransaction {
        RegistrationTokenEntity.find { RegistrationTokens.user eq userId }
            .forEach { it.delete() }
    }
}
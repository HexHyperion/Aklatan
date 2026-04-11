package com.hexhyperion.aklatan.api.auth.tokens

import com.hexhyperion.aklatan.db.*
import com.hexhyperion.aklatan.utility.Hasher
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import kotlin.time.Clock
import kotlin.time.Instant

class PasswordResetTokenRepository {
    suspend fun save(userId: Int, tokenHash: String, expiresAt: Instant) = withTransaction {
        PasswordResetTokenEntity.new {
            this.user = UserEntity.findById(userId) ?: throw IllegalArgumentException("User not found")
            this.tokenHash = tokenHash
            this.expiresAt = expiresAt
        }
    }

    suspend fun find(passwordResetToken: String): PasswordResetToken? = withTransaction {
        PasswordResetTokenEntity
            .find { PasswordResetTokens.tokenHash eq Hasher.sha256Hash(passwordResetToken) }
            .firstOrNull()
            ?.toPasswordResetToken()
    }

    suspend fun findAllForUser(userId: Int): List<PasswordResetToken> = withTransaction {
        PasswordResetTokenEntity
            .find { PasswordResetTokens.user eq userId }
            .map { it.toPasswordResetToken() }
    }

    suspend fun deleteAllExpired() = withTransaction {
        PasswordResetTokenEntity.find { PasswordResetTokens.expiresAt less Clock.System.now() }
            .forEach { it.delete() }
    }

    suspend fun delete(passwordResetToken: String) = withTransaction {
        PasswordResetTokenEntity.find { PasswordResetTokens.tokenHash eq Hasher.sha256Hash(passwordResetToken) }
            .forEach { it.delete() }
    }

    suspend fun deleteAllForUser(userId: Int) = withTransaction {
        PasswordResetTokenEntity.find { PasswordResetTokens.user eq userId }
            .forEach { it.delete() }
    }
}
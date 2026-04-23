package com.hexhyperion.aklatan.api.auth.tokens

import com.hexhyperion.aklatan.db.*
import com.hexhyperion.aklatan.utility.Hasher
import com.hexhyperion.aklatan.utility.exception.UserNotFoundException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import kotlin.time.Clock
import kotlin.time.Instant

class PasswordResetTokenRepository {
    suspend fun save(userId: Int, tokenHash: String, expiresAt: Instant) = withTransaction {
        PasswordResetTokenEntity.new {
            this.user = UserEntity.findById(userId) ?: throw UserNotFoundException()
            this.tokenHash = tokenHash
            this.expiresAt = expiresAt
        }
    }

    suspend fun find(passwordResetToken: String): PasswordResetToken? = withTransaction {
        PasswordResetTokenEntity.find { PasswordResetTokens.tokenHash eq Hasher.sha256Hash(passwordResetToken) }
            .firstOrNull()
            ?.toPasswordResetToken()
    }

    suspend fun deleteAllExpired() = withTransaction {
        PasswordResetTokens.deleteWhere { PasswordResetTokens.expiresAt less Clock.System.now() }
    }

    suspend fun delete(passwordResetToken: String) = withTransaction {
        PasswordResetTokens.deleteWhere { PasswordResetTokens.tokenHash eq Hasher.sha256Hash(passwordResetToken) }
    }

    suspend fun deleteAllForUser(userId: Int) = withTransaction {
        PasswordResetTokens.deleteWhere { PasswordResetTokens.user eq userId }
    }
}
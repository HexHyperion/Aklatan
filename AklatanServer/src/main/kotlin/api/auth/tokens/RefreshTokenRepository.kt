package com.hexhyperion.aklatan.api.auth.tokens

import com.hexhyperion.aklatan.db.*
import com.hexhyperion.aklatan.utility.Hasher
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import kotlin.time.Clock
import kotlin.time.Instant

class RefreshTokenRepository {
    suspend fun save(userId: Int, tokenHash: String, expiresAt: Instant) = withTransaction {
        RefreshTokenEntity.new {
            this.user = UserEntity.findById(userId) ?: throw IllegalArgumentException("User not found")
            this.tokenHash = tokenHash
            this.expiresAt = expiresAt
        }
    }

    suspend fun find(refreshToken: String): RefreshToken? = withTransaction {
        RefreshTokenEntity
            .find { RefreshTokens.tokenHash eq Hasher.sha256Hash(refreshToken) }
            .firstOrNull()
            ?.toRefreshToken()
    }

    suspend fun findAllForUser(userId: Int): List<RefreshToken> = withTransaction {
        RefreshTokenEntity
            .find { RefreshTokens.user eq userId }
            .map { it.toRefreshToken() }
    }

    suspend fun deleteAllExpired() = withTransaction {
        RefreshTokenEntity.find { RefreshTokens.expiresAt less Clock.System.now() }
            .forEach { it.delete() }
    }

    suspend fun delete(refreshToken: String) = withTransaction {
        RefreshTokenEntity.find { RefreshTokens.tokenHash eq Hasher.sha256Hash(refreshToken) }
            .forEach { it.delete() }
    }

    suspend fun deleteAllForUser(userId: Int) = withTransaction {
        RefreshTokenEntity.find { RefreshTokens.user eq userId }
            .forEach { it.delete() }
    }
}
package com.hexhyperion.aklatan.api.auth.tokens

import com.hexhyperion.aklatan.db.*
import com.hexhyperion.aklatan.utility.Hasher
import com.hexhyperion.aklatan.utility.exception.UserNotFoundException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import kotlin.time.Clock
import kotlin.time.Instant

class RefreshTokenRepository {
    suspend fun save(userId: Int, tokenHash: String, expiresAt: Instant) = withTransaction {
        RefreshTokenEntity.new {
            this.user = UserEntity.findById(userId) ?: throw UserNotFoundException()
            this.tokenHash = tokenHash
            this.expiresAt = expiresAt
        }
    }

    suspend fun find(refreshToken: String): RefreshToken? = withTransaction {
        RefreshTokenEntity.find { RefreshTokens.tokenHash eq Hasher.sha256Hash(refreshToken) }
            .firstOrNull()
            ?.toRefreshToken()
    }

    suspend fun deleteAllExpired() = withTransaction {
        RefreshTokens.deleteWhere { RefreshTokens.expiresAt less Clock.System.now() }
    }

    suspend fun delete(refreshToken: String) = withTransaction {
        RefreshTokens.deleteWhere { RefreshTokens.tokenHash eq Hasher.sha256Hash(refreshToken) }
    }

    suspend fun deleteAllForUser(userId: Int) = withTransaction {
        RefreshTokens.deleteWhere { RefreshTokens.user eq userId }
    }
}
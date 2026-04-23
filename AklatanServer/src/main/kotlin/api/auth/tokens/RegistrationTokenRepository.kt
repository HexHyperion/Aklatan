package com.hexhyperion.aklatan.api.auth.tokens

import com.hexhyperion.aklatan.db.*
import com.hexhyperion.aklatan.utility.Hasher
import com.hexhyperion.aklatan.utility.exception.UserNotFoundException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import kotlin.time.Clock
import kotlin.time.Instant

class RegistrationTokenRepository {
    suspend fun save(userId: Int, tokenHash: String, expiresAt: Instant) = withTransaction {
        RegistrationTokenEntity.new {
            this.user = UserEntity.findById(userId) ?: throw UserNotFoundException()
            this.tokenHash = tokenHash
            this.expiresAt = expiresAt
        }
    }

    suspend fun find(registrationToken: String): RegistrationToken? = withTransaction {
        RegistrationTokenEntity.find { RegistrationTokens.tokenHash eq Hasher.sha256Hash(registrationToken) }
            .firstOrNull()
            ?.toRegistrationToken()
    }

    suspend fun deleteAllExpired() = withTransaction {
        RegistrationTokens.deleteWhere { RegistrationTokens.expiresAt less Clock.System.now() }
    }

    suspend fun delete(registrationToken: String) = withTransaction {
        RegistrationTokens.deleteWhere { RegistrationTokens.tokenHash eq Hasher.sha256Hash(registrationToken) }
    }

    suspend fun deleteAllForUser(userId: Int) = withTransaction {
        RegistrationTokens.deleteWhere { RegistrationTokens.user eq userId }
    }
}
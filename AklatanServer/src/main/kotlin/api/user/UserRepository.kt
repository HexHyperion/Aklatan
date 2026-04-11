package com.hexhyperion.aklatan.api.user

import com.hexhyperion.aklatan.db.*
import org.jetbrains.exposed.v1.core.eq
import kotlin.time.Clock

class UserRepository {
    suspend fun create(email: String, name: String, passwordHash: String, roleId: Int): User = withTransaction {
        return@withTransaction UserEntity.new {
            this.email = email
            this.name = name
            this.passwordHash = passwordHash
            this.role = RoleEntity.findById(roleId)!!
            this.registeredAt = Clock.System.now()
        }.toUser()
    }

    suspend fun findById(id: Int): User? = withTransaction {
        UserEntity.findById(id)
            ?.toUser()
    }

    suspend fun findByEmail(email: String): User? = withTransaction {
        UserEntity.find { Users.email eq email }
            .firstOrNull()
            ?.toUser()
    }

    suspend fun findAll(): List<User> = withTransaction {
        UserEntity.all()
            .map { it.toUser() }
    }

    suspend fun findIdByEmail(email: String): Int? = withTransaction {
        UserEntity.find { Users.email eq email }
            .firstOrNull()
            ?.id?.value
    }

    suspend fun updateVerified(id: Int) = withTransaction {
        UserEntity.findById(id)?.let {
            it.verified = true
            it.flush()
        }
    }

    suspend fun updatePasswordHash(id: Int, newHash: String) = withTransaction {
        UserEntity.findById(id)?.let {
            it.passwordHash = newHash
            it.flush()
        }
    }

    suspend fun delete(id: Int) = withTransaction {
        UserEntity.findById(id)?.delete()
    }
}
package com.hexhyperion.aklatan.api.user

import com.hexhyperion.aklatan.db.*
import org.jetbrains.exposed.v1.core.eq
import kotlin.time.Clock

class UserRepository {
    suspend fun create(email: String, name: String, passwordHash: String, role: String): User = withTransaction {
        return@withTransaction UserEntity.new {
            this.email = email
            this.name = name
            this.passwordHash = passwordHash
            this.role = RoleEntity.findByName(role) ?: throw IllegalArgumentException("Role not found")
            this.registeredAt = Clock.System.now()
        }.toUser()
    }

    suspend fun findById(id: Int): User? = withTransaction {
        UserEntity.find { Users.id eq id }
            .firstOrNull()
            ?.toUser()
    }

    suspend fun findByEmail(email: String): User? = withTransaction {
        UserEntity.find { Users.email eq email }
            .firstOrNull()
            ?.toUser()
    }

    suspend fun findIdByEmail(email: String): Int? = withTransaction {
        UserEntity.find { Users.email eq email }
            .firstOrNull()
            ?.id?.value
    }

    suspend fun delete(id: Int) = withTransaction {
        UserEntity.findById(id)?.delete()
    }
}
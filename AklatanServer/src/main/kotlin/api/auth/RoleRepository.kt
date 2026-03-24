package com.hexhyperion.aklatan.api.auth

import com.hexhyperion.aklatan.db.RoleEntity
import com.hexhyperion.aklatan.db.Roles
import com.hexhyperion.aklatan.db.withTransaction
import org.jetbrains.exposed.v1.core.eq

class RoleRepository {
    suspend fun create(name: String, permissionLevel: Int): RoleEntity = withTransaction {
        RoleEntity.new {
            this.name = name
            this.permissionLevel = permissionLevel
        }
    }

    suspend fun findByName(name: String): RoleEntity? = withTransaction {
        RoleEntity.find { Roles.name eq name }
            .firstOrNull()
    }

    suspend fun findById(id: Int): RoleEntity? = withTransaction {
        RoleEntity.find { Roles.id eq id }
            .firstOrNull()
    }
}
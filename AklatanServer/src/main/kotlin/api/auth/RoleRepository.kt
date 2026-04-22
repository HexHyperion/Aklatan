package com.hexhyperion.aklatan.api.auth

import com.hexhyperion.aklatan.db.Role
import com.hexhyperion.aklatan.db.RoleEntity
import com.hexhyperion.aklatan.db.Roles
import com.hexhyperion.aklatan.db.withTransaction
import org.jetbrains.exposed.v1.core.eq

class RoleRepository {
    suspend fun create(name: String): Role = withTransaction {
        RoleEntity.new { this.name = name }
            .toRole()
    }

    suspend fun findByName(name: String): Role? = withTransaction {
        RoleEntity.find { Roles.name eq name }
            .firstOrNull()
            ?.toRole()
    }

    suspend fun findById(id: Int): Role? = withTransaction {
        RoleEntity.findById(id)
            ?.toRole()
    }

    suspend fun findAll(): List<Role> = withTransaction {
        RoleEntity.all()
            .map{ it.toRole() }
    }
}
package com.hexhyperion.aklatan.api.auth

import com.hexhyperion.aklatan.db.Role
import com.hexhyperion.aklatan.utility.exception.RoleNotFoundException

class RoleService(private val roleRepository: RoleRepository) {
    suspend fun create(name: String, permissionLevel: Int): Role {
        return roleRepository.create(name, permissionLevel).toRole()
    }

    suspend fun getByName(name: String): Role {
        val roleEntity = roleRepository.findByName(name) ?: throw RoleNotFoundException()
        return roleEntity.toRole()
    }

    suspend fun getById(id: Int): Role {
        val roleEntity = roleRepository.findById(id) ?: throw RoleNotFoundException()
        return roleEntity.toRole()
    }
}
package com.hexhyperion.aklatan.api.auth

import com.hexhyperion.aklatan.db.Role

class RoleService(private val roleRepository: RoleRepository) {
    suspend fun create(name: String, permissionLevel: Int): Role {
        return roleRepository.create(name, permissionLevel).toRole()
    }

    suspend fun getByName(name: String): Role? {
        return roleRepository.findByName(name)?.toRole()
    }

    suspend fun getById(id: Int): Role? {
        return roleRepository.findById(id)?.toRole()
    }
}
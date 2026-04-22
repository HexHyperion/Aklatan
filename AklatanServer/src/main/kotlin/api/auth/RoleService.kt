package com.hexhyperion.aklatan.api.auth

import com.hexhyperion.aklatan.db.Role
import com.hexhyperion.aklatan.utility.exception.RoleNotFoundException

class RoleService(private val roleRepository: RoleRepository) {
    suspend fun create(name: String): Role {
        return roleRepository.create(name)
    }

    suspend fun getByName(name: String): Role {
        return roleRepository.findByName(name) ?: throw RoleNotFoundException()
    }

    suspend fun getById(id: Int): Role {
        return roleRepository.findById(id) ?: throw RoleNotFoundException()
    }

    suspend fun getAll(): List<Role> {
        return roleRepository.findAll()
    }
}
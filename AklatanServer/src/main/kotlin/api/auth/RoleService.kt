package com.hexhyperion.aklatan.api.auth

import com.hexhyperion.aklatan.db.Role
import com.hexhyperion.aklatan.utility.exception.RoleNotFoundException

class RoleService(private val roleRepository: RoleRepository) {
    suspend fun getNameById(id: Int): String {
        return roleRepository.findById(id)?.name ?: throw RoleNotFoundException()
    }

    suspend fun getAll(): List<Role> {
        return roleRepository.findAll()
    }
}
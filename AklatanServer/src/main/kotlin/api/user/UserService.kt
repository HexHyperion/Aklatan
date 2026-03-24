package com.hexhyperion.api.user

import com.hexhyperion.api.auth.RoleRepository
import com.hexhyperion.db.User
import com.hexhyperion.utility.Hasher

class UserService(private val userRepository: UserRepository, private val roleRepository: RoleRepository) {
    suspend fun create(email: String, name: String, password: String, role: String): User {
        return userRepository.create(email, name, Hasher.bcryptHash(password), role)
    }

    suspend fun getById(id: Int): User? {
        return userRepository.findById(id)
    }

    suspend fun getByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    suspend fun getIdByEmail(email: String): Int? {
        return userRepository.findIdByEmail(email)
    }

    suspend fun getRoleNameById(id: Int): String? {
        val user = userRepository.findById(id) ?: return null
        return user.roleId.let { roleRepository.findById(it)?.name }
    }

    suspend fun authenticate(email: String, password: String): User? {
        val user = userRepository.findByEmail(email) ?: return null
        return if (Hasher.bcryptVerify(password, user.passwordHash)) user else null
    }
}
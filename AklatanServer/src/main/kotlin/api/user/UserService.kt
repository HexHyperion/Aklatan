package com.hexhyperion.aklatan.api.user

import com.hexhyperion.aklatan.api.auth.RoleRepository
import com.hexhyperion.aklatan.db.User
import com.hexhyperion.aklatan.utility.Hasher
import com.hexhyperion.aklatan.utility.exception.BadCredentialsException
import com.hexhyperion.aklatan.utility.exception.RoleNotFoundException
import com.hexhyperion.aklatan.utility.exception.UserNotFoundException

class UserService(private val userRepository: UserRepository, private val roleRepository: RoleRepository) {
    suspend fun create(email: String, name: String, password: String, role: String): User {
        val roleId = roleRepository.findByName(role)?.id?.value ?: throw RoleNotFoundException()
        return userRepository.create(email, name, Hasher.bcryptHash(password), roleId)
    }

    suspend fun getById(id: Int): User {
        return userRepository.findById(id) ?: throw UserNotFoundException()
    }

    suspend fun getByIdOrNull(id: Int): User? {
        return userRepository.findById(id)
    }

    suspend fun getByEmail(email: String): User {
        return userRepository.findByEmail(email) ?: throw UserNotFoundException()
    }

    suspend fun getByEmailOrNull(email: String): User? {
        return userRepository.findByEmail(email)
    }

    suspend fun getIdByEmail(email: String): Int {
        return userRepository.findIdByEmail(email) ?: throw UserNotFoundException()
    }

    suspend fun getIdByEmailOrNull(email: String): Int? {
        return userRepository.findIdByEmail(email)
    }

    suspend fun getRoleNameById(id: Int): String {
        val user = userRepository.findById(id) ?: throw UserNotFoundException()
        return user.roleId.let { roleRepository.findById(it)?.name ?: throw RoleNotFoundException() }
    }

    suspend fun checkExistsByEmail(email: String): Boolean {
        return userRepository.findByEmail(email) != null
    }

    suspend fun authenticate(email: String, password: String): User {
        val user = userRepository.findByEmail(email) ?: throw BadCredentialsException()
        return if (Hasher.bcryptVerify(password, user.passwordHash)) user else throw BadCredentialsException()
    }

    suspend fun verifyEmail(id: Int) {
        userRepository.updateVerified(id)
    }

    suspend fun resetPassword(id: Int, password: String) {
        userRepository.updatePasswordHash(id, Hasher.bcryptHash(password))
    }

    suspend fun delete(id: Int) {
        userRepository.delete(id)
    }
}
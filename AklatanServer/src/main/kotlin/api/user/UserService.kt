package com.hexhyperion.aklatan.api.user

import com.hexhyperion.aklatan.api.auth.RoleRepository
import com.hexhyperion.aklatan.db.User
import com.hexhyperion.aklatan.db.UserReadable
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

    suspend fun getByIdReadable(id: Int): UserReadable {
        val userRaw = userRepository.findById(id) ?: throw UserNotFoundException()
        val userRole = roleRepository.findById(userRaw.roleId)?.name ?: throw RoleNotFoundException()
        val userId = userRepository.findIdByEmail(userRaw.email) ?: throw UserNotFoundException()
        return UserReadable(
            id = userId,
            name = userRaw.name,
            email = userRaw.email,
            role = userRole,
            registeredAt = userRaw.registeredAt,
            verified = userRaw.verified
        )
    }

    suspend fun getAllReadable(): List<UserReadable> {
        val usersRaw = userRepository.findAll()
        val users = mutableListOf<UserReadable>()
        for (userRaw in usersRaw) {
            val userRole = roleRepository.findById(userRaw.roleId)?.name ?: throw RoleNotFoundException()
            val userId = userRepository.findIdByEmail(userRaw.email) ?: throw UserNotFoundException()
            users.add(UserReadable(
                id = userId,
                name = userRaw.name,
                email = userRaw.email,
                role = userRole,
                registeredAt = userRaw.registeredAt,
                verified = userRaw.verified
            ))
        }
        return users
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

    suspend fun checkExistsById(id: Int): Boolean {
        return userRepository.findById(id) != null
    }

    suspend fun checkExistsByEmail(email: String): Boolean {
        return userRepository.findByEmail(email) != null
    }

    suspend fun authenticate(email: String, password: String): User {
        val user = userRepository.findByEmail(email) ?: throw BadCredentialsException()
        return if (Hasher.bcryptVerify(password, user.passwordHash)) user else throw BadCredentialsException()
    }

    suspend fun verifyEmail(id: Int, verified: Boolean = true) {
        userRepository.findById(id) ?: throw UserNotFoundException()
        userRepository.updateVerified(id, verified)
    }

    suspend fun changeName(id: Int, name: String) {
        userRepository.findById(id) ?: throw UserNotFoundException()
        userRepository.updateName(id, name)
    }

    suspend fun changeEmail(id: Int, email: String) {
        userRepository.findById(id) ?: throw UserNotFoundException()
        userRepository.updateEmail(id, email)
    }

    suspend fun changePassword(id: Int, password: String) {
        userRepository.findById(id) ?: throw UserNotFoundException()
        userRepository.updatePasswordHash(id, Hasher.bcryptHash(password))
    }

    suspend fun changeRole(id: Int, role: String) {
        val roleId = roleRepository.findByName(role)?.id?.value ?: throw RoleNotFoundException()
        userRepository.findById(id) ?: throw UserNotFoundException()
        userRepository.updateRoleId(id, roleId)
    }

    suspend fun delete(id: Int) {
        userRepository.findById(id) ?: throw UserNotFoundException()
        userRepository.delete(id)
    }
}
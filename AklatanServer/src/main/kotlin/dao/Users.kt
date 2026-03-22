package com.hexhyperion.dao

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object Users : IntIdTable("users") {
    val name = text("name")
    val email = text("email").uniqueIndex()
    val passwordHash = text("password_hash")
    val roleId = integer("role_id").references(Roles.id)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    var name by Users.name
    var email by Users.email
    var passwordHash by Users.passwordHash
    var roleId by Users.roleId

    companion object : IntEntityClass<User>(Users)
}
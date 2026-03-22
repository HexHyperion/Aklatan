package com.hexhyperion.dao

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object Roles : IntIdTable() {
    val name = text("name").uniqueIndex()
    val permissionLevel = integer("permission_level")
}

class Role(id: EntityID<Int>) : IntEntity(id) {
    var name by Roles.name
    var permissionLevel by Roles.permissionLevel

    companion object : IntEntityClass<Role>(Roles)
}
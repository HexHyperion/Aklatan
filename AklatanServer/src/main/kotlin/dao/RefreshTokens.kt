package com.hexhyperion.dao

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

object RefreshTokens : IntIdTable() {
    val userId = integer("user_id").references(Users.id)
    val tokenHash = text("token_hash").uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val revoked = bool("revoked").default(false)
}

class RefreshToken(id: EntityID<Int>) : IntEntity(id) {
    var userId by RefreshTokens.userId
    var tokenHash by RefreshTokens.tokenHash
    var expiresAt by RefreshTokens.expiresAt
    var revoked by RefreshTokens.revoked

    companion object : IntEntityClass<RefreshToken>(RefreshTokens)
}
package com.hexhyperion.dao

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

object Reservations : IntIdTable() {
    val userId = integer("user_id").references(Users.id)
    val bookId = integer("book_id").references(Books.id)
    val reservedAt = timestamp("reserved_at")
    val expiresAt = timestamp("expires_at")
    val cancelled = bool("cancelled").default(false)
}

class Reservation(id: EntityID<Int>) : IntEntity(id) {
    var userId by Reservations.userId
    var bookId by Reservations.bookId
    var reservedAt by Reservations.reservedAt
    var expiresAt by Reservations.expiresAt
    var cancelled by Reservations.cancelled

    companion object : IntEntityClass<Reservation>(Reservations)
}

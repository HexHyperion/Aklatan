package com.hexhyperion.dao

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

object Borrows : IntIdTable() {
    val userId = integer("user_id").references(Users.id)
    val bookId = integer("book_id").references(Books.id)
    val borrowedAt = timestamp("borrowed_at")
    val endsAt = timestamp("ends_at")
    val returnedAt = timestamp("returned_at").nullable()
}

class Borrow(id: EntityID<Int>) : IntEntity(id) {
    var userId by Borrows.userId
    var bookId by Borrows.bookId
    var borrowedAt by Borrows.borrowedAt
    var endsAt by Borrows.endsAt
    var returnedAt by Borrows.returnedAt

    companion object : IntEntityClass<Borrow>(Borrows)
}
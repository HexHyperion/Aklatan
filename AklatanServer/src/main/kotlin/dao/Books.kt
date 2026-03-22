package com.hexhyperion.dao

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object Books : IntIdTable("books") {
    val isbn = text("isbn").uniqueIndex()
    val title = text("title").nullable()
    val author = text("author").nullable()
    val year = text("year").nullable()
}

class Book(id: EntityID<Int>) : IntEntity(id) {
    var isbn by Books.isbn
    var title by Books.title
    var author by Books.author
    var year by Books.year

    companion object : IntEntityClass<Book>(Books)
}
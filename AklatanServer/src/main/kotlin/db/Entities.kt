package com.hexhyperion.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

object Roles : IntIdTable() {
    val name = text("name").uniqueIndex()
    val permissionLevel = integer("permission_level")
}

class Role(id: EntityID<Int>) : IntEntity(id) {
    var name by Roles.name
    var permissionLevel by Roles.permissionLevel

    companion object : IntEntityClass<Role>(Roles)
}

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
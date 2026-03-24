package com.hexhyperion.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

object Roles : IntIdTable() {
    val name = text("name").uniqueIndex()
    val permissionLevel = integer("permission_level")
}

class RoleEntity(id: EntityID<Int>) : IntEntity(id) {
    var name by Roles.name
    var permissionLevel by Roles.permissionLevel

    fun toRole(): Role = Role(name, permissionLevel)

    companion object : IntEntityClass<RoleEntity>(Roles) {
        fun findByName(name: String): RoleEntity? = find { Roles.name eq name }.firstOrNull()
    }
}

object Users : IntIdTable() {
    val name = text("name")
    val email = text("email").uniqueIndex()
    val passwordHash = text("password_hash")
    val role = reference("role_id", Roles)
}

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    var name by Users.name
    var email by Users.email
    var passwordHash by Users.passwordHash
    var role by RoleEntity referencedOn Users.role

    fun toUser(): User = User(name, email, passwordHash, role.id.value)

    companion object : IntEntityClass<UserEntity>(Users)
}

object RefreshTokens : IntIdTable() {
    val user = reference("user_id", Users)
    val tokenHash = text("token_hash").uniqueIndex()
    val expiresAt = timestamp("expires_at")
}

class RefreshTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    var user by UserEntity referencedOn RefreshTokens.user
    var tokenHash by RefreshTokens.tokenHash
    var expiresAt by RefreshTokens.expiresAt

    fun toRefreshToken(): RefreshToken = RefreshToken(user.id.value, tokenHash, expiresAt)

    companion object : IntEntityClass<RefreshTokenEntity>(RefreshTokens)
}

object Books : IntIdTable() {
    val isbn = text("isbn").uniqueIndex()
    val title = text("title").nullable()
    val author = text("author").nullable()
    val year = text("year").nullable()
}

class BookEntity(id: EntityID<Int>) : IntEntity(id) {
    var isbn by Books.isbn
    var title by Books.title
    var author by Books.author
    var year by Books.year

    fun toBook(): Book = Book(isbn, title, author, year)

    companion object : IntEntityClass<BookEntity>(Books)
}

object Reservations : IntIdTable() {
    val user = reference("user_id", Users)
    val book = reference("book_id", Books)
    val reservedAt = timestamp("reserved_at")
    val expiresAt = timestamp("expires_at")
    val cancelled = bool("cancelled").default(false)
}

class ReservationEntity(id: EntityID<Int>) : IntEntity(id) {
    var user by UserEntity referencedOn Reservations.user
    var book by BookEntity referencedOn Reservations.book
    var reservedAt by Reservations.reservedAt
    var expiresAt by Reservations.expiresAt
    var cancelled by Reservations.cancelled

    fun toReservation(): Reservation = Reservation(user.id.value, book.id.value, reservedAt, expiresAt, cancelled)

    companion object : IntEntityClass<ReservationEntity>(Reservations)
}

object Borrows : IntIdTable() {
    val user = reference("user_id", Users)
    val book = reference("book_id", Books)
    val borrowedAt = timestamp("borrowed_at")
    val endsAt = timestamp("ends_at")
    val returnedAt = timestamp("returned_at").nullable()
}

class BorrowEntity(id: EntityID<Int>) : IntEntity(id) {
    var user by UserEntity referencedOn Borrows.user
    var book by BookEntity referencedOn Borrows.book
    var borrowedAt by Borrows.borrowedAt
    var endsAt by Borrows.endsAt
    var returnedAt by Borrows.returnedAt

    fun toBorrow(): Borrow = Borrow(user.id.value, book.id.value, borrowedAt, endsAt, returnedAt)

    companion object : IntEntityClass<BorrowEntity>(Borrows)
}
package com.hexhyperion.aklatan.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.time
import org.jetbrains.exposed.v1.datetime.timestamp

object Roles : IntIdTable() {
    val name = text("name").uniqueIndex()
}

class RoleEntity(id: EntityID<Int>) : IntEntity(id) {
    var name by Roles.name

    fun toRole(): Role = Role(name)

    companion object : IntEntityClass<RoleEntity>(Roles)
}

object Users : IntIdTable() {
    val name = text("name")
    val email = text("email").uniqueIndex()
    val passwordHash = text("password_hash")
    val role = reference("role_id", Roles)
    val registeredAt = timestamp("registered_at")
    val verified = bool("verified").default(false)
}

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    var name by Users.name
    var email by Users.email
    var passwordHash by Users.passwordHash
    var role by RoleEntity referencedOn Users.role
    var registeredAt by Users.registeredAt
    var verified by Users.verified

    fun toUser(): User = User(name, email, passwordHash, role.id.value, registeredAt, verified)

    companion object : IntEntityClass<UserEntity>(Users)
}

object RegistrationTokens: IntIdTable("registration_tokens") {
    val user = reference("user_id", Users)
    val tokenHash = text("token_hash").uniqueIndex()
    val expiresAt = timestamp("expires_at")
}

class RegistrationTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    var user by UserEntity referencedOn RegistrationTokens.user
    var tokenHash by RegistrationTokens.tokenHash
    var expiresAt by RegistrationTokens.expiresAt

    fun toRegistrationToken(): RegistrationToken = RegistrationToken(user.id.value, tokenHash, expiresAt)

    companion object : IntEntityClass<RegistrationTokenEntity>(RegistrationTokens)
}

object PasswordResetTokens: IntIdTable("password_reset_tokens") {
    val user = reference("user_id", Users)
    val tokenHash = text("token_hash").uniqueIndex()
    val expiresAt = timestamp("expires_at")
}

class PasswordResetTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    var user by UserEntity referencedOn PasswordResetTokens.user
    var tokenHash by PasswordResetTokens.tokenHash
    var expiresAt by PasswordResetTokens.expiresAt

    fun toPasswordResetToken(): PasswordResetToken = PasswordResetToken(user.id.value, tokenHash, expiresAt)

    companion object : IntEntityClass<PasswordResetTokenEntity>(PasswordResetTokens)
}

object RefreshTokens : IntIdTable("refresh_tokens") {
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

object OpenHours : IntIdTable("open_hours") {
    val weekDay = integer("week_day").uniqueIndex()
    val openTime = time("open_time").nullable()
    val closeTime = time("close_time").nullable()
}

class OpenHourEntity(id: EntityID<Int>) : IntEntity(id) {
    var weekDay by OpenHours.weekDay
    var openTime by OpenHours.openTime
    var closeTime by OpenHours.closeTime

    fun toOpenHour(): OpenHour = OpenHour(weekDay, openTime, closeTime)

    companion object : IntEntityClass<OpenHourEntity>(OpenHours)
}

object OpenHourExceptions : IntIdTable("open_hour_exceptions") {
    val date = date("date").uniqueIndex()
    val openTime = time("open_time").nullable()
    val closeTime = time("close_time").nullable()
    val comment = text("comment").nullable()
}

class OpenHourExceptionEntity(id: EntityID<Int>) : IntEntity(id) {
    var date by OpenHourExceptions.date
    var openTime by OpenHourExceptions.openTime
    var closeTime by OpenHourExceptions.closeTime
    var comment by OpenHourExceptions.comment

    fun toOpenHourException(): OpenHourException = OpenHourException(date, openTime, closeTime, comment)

    companion object : IntEntityClass<OpenHourExceptionEntity>(OpenHourExceptions)
}
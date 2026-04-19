package com.hexhyperion.aklatan.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelSuspendTransaction
import kotlin.time.Instant

suspend fun <T> withTransaction(block: suspend JdbcTransaction.() -> T): T = withContext(Dispatchers.IO) {
    inTopLevelSuspendTransaction { block() }
}

@Serializable
data class Role (
    val name: String
)

@Serializable
data class User (
    val name: String,
    val email: String,
    val passwordHash: String,
    val roleId: Int,
    val registeredAt: Instant,
    val verified: Boolean
)

@Serializable
data class UserReadable (
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val registeredAt: Instant,
    val verified: Boolean
)

@Serializable
data class RegistrationToken (
    val userId: Int,
    val tokenHash: String,
    val expiresAt: Instant
)

@Serializable
data class PasswordResetToken (
    val userId: Int,
    val tokenHash: String,
    val expiresAt: Instant
)

@Serializable
data class RefreshToken (
    val userId: Int,
    val tokenHash: String,
    val expiresAt: Instant
)

@Serializable
data class Book (
    val isbn: String,
    val title: String?,
    val author: String?,
    val year: String?
)

@Serializable
data class Reservation (
    val isbn: String,
    val userId: Int,
    val reservedAt: Instant,
    val expiresAt: Instant,
    val canceled: Boolean
)

@Serializable
data class Borrow (
    val bookId: Int,
    val userId: Int,
    val borrowedAt: Instant,
    val endsAt: Instant,
    val returnedAt: Instant?
)

@Serializable
data class OpenHour (
    val weekDay: Int,
    val openTime: LocalTime?,
    val closeTime: LocalTime?
)

@Serializable
data class OpenHourException (
    val date: LocalDate,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
    val comment: String?
)
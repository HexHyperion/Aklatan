package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.db.*
import com.hexhyperion.aklatan.utility.exception.UserNotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import kotlin.time.Clock
import kotlin.time.Instant

class ReservationRepository {
    suspend fun create(isbn: String, userId: Int, expiresAt: Instant): Reservation = withTransaction {
        return@withTransaction ReservationEntity.new {
            this.isbn = isbn
            this.user = UserEntity.findById(userId) ?: throw UserNotFoundException()
            this.reservedAt = Clock.System.now()
            this.expiresAt = expiresAt
        }.toReservation()
    }

    suspend fun findById(id: Int): Reservation? = withTransaction {
        return@withTransaction ReservationEntity.findById(id)?.toReservation()
    }

    suspend fun findByUserId(userId: Int): List<Reservation> = withTransaction {
        return@withTransaction ReservationEntity.find { Reservations.user eq userId }
            .map { it.toReservation() }
    }

    suspend fun findByIsbn(isbn: String): List<Reservation> = withTransaction {
        return@withTransaction ReservationEntity.find { Reservations.isbn eq isbn }
            .map { it.toReservation() }
    }

    suspend fun findActiveByIsbn(isbn: String): List<Reservation> = withTransaction {
        val now = Clock.System.now()
        return@withTransaction ReservationEntity.find {
            (Reservations.isbn eq isbn) and
            (Reservations.canceled eq false) and
            (Reservations.expiresAt greaterEq now)
        }.map { it.toReservation() }
    }

    suspend fun findActiveOrderedByDateByIsbn(isbn: String): List<Reservation> = withTransaction {
        val now = Clock.System.now()
        return@withTransaction ReservationEntity.find {
            (Reservations.isbn eq isbn) and
            (Reservations.canceled eq false) and
            (Reservations.expiresAt greaterEq now)
        }.orderBy(Reservations.reservedAt to SortOrder.ASC)
        .map { it.toReservation() }
    }

    suspend fun findAll(): List<Reservation> = withTransaction {
        return@withTransaction ReservationEntity.all().map { it.toReservation() }
    }

    suspend fun findActiveIdByIsbnAndUserId(isbn: String, userId: Int): Int? = withTransaction {
        return@withTransaction ReservationEntity.find {
            (Reservations.isbn eq isbn) and
            (Reservations.user eq userId) and
            (Reservations.expiresAt greaterEq Clock.System.now()) and
            (Reservations.canceled eq false)
        }
        .firstOrNull()
        ?.id?.value
    }

    suspend fun updateCanceled(id: Int, canceled: Boolean = true) = withTransaction {
        ReservationEntity.findById(id)?.apply { this.canceled = canceled }
    }
}
package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.api.book.BookRepository
import com.hexhyperion.aklatan.db.Reservation
import com.hexhyperion.aklatan.db.withTransaction
import com.hexhyperion.aklatan.utility.exception.BookAlreadyReservedException
import com.hexhyperion.aklatan.utility.exception.BookNotFoundException
import com.hexhyperion.aklatan.utility.exception.ReservationNotFoundException
import io.ktor.server.config.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class ReservationService (
    private val reservationRepository: ReservationRepository,
    private val bookRepository: BookRepository,
    private val config: ApplicationConfig
) {
    suspend fun reserve(isbn: String, userId: Int): Reservation {
        return withTransaction {
            if (reservationRepository.findActiveByIsbn(isbn).find { it.userId == userId } != null) {
                throw BookAlreadyReservedException()
            }
            if (bookRepository.findByIsbn(isbn).isEmpty()) {
                throw BookNotFoundException()
            }
            val reservationDays = config.property("books.reservationDurationDays").getString().toInt()
            val expiresAt = Clock.System.now() + reservationDays.days
            return@withTransaction reservationRepository.create(isbn, userId, expiresAt)
        }
    }

    suspend fun reserveMany(isbns: Set<String>, userId: Int): List<Reservation> {
        return withTransaction {
            val books = bookRepository.findByIsbns(isbns)
            val currentReservationIsbns = reservationRepository.findActiveByIsbns(isbns)
                .filter { it.userId == userId }
                .map { it.isbn }

            isbns.forEach { isbn ->
                if (isbn in currentReservationIsbns) {
                    throw BookAlreadyReservedException()
                }
                if (books.find { it.isbn == isbn } == null) {
                    throw BookNotFoundException()
                }
            }
            val reservationDays = config.property("books.reservationDurationDays").getString().toInt()
            val expiresAt = Clock.System.now() + reservationDays.days
            return@withTransaction isbns.map { reservationRepository.create(it, userId, expiresAt) }
        }
    }

    suspend fun getById(id: Int): Reservation {
        return reservationRepository.findById(id) ?: throw ReservationNotFoundException()
    }

    suspend fun getAllForUser(userId: Int): List<Reservation> {
        val reservations = reservationRepository.findByUserId(userId)
        if (reservations.isEmpty()) throw ReservationNotFoundException()
        return reservations
    }

    suspend fun getActiveById(id: Int): Reservation {
        return reservationRepository.findActiveById(id) ?: throw ReservationNotFoundException()
    }

    suspend fun getAllActivePrioritizedForIsbn(isbn: String): List<Reservation> {
        val reservations = reservationRepository.findActiveOrderedByDateByIsbn(isbn)
        if (reservations.isEmpty()) throw ReservationNotFoundException()
        return reservations
    }

    suspend fun getAll(): List<Reservation> {
        return reservationRepository.findAll()
    }

    suspend fun cancel(id: Int) {
        reservationRepository.updateCanceled(id) ?: throw ReservationNotFoundException()
    }
}
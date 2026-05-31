package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.api.book.BookRepository
import com.hexhyperion.aklatan.api.user.UserRepository
import com.hexhyperion.aklatan.db.Book
import com.hexhyperion.aklatan.db.Reservation
import com.hexhyperion.aklatan.db.ReservationReadable
import com.hexhyperion.aklatan.db.withTransaction
import com.hexhyperion.aklatan.utility.exception.BookAlreadyReservedException
import com.hexhyperion.aklatan.utility.exception.BookNotFoundException
import com.hexhyperion.aklatan.utility.exception.ReservationNotFoundException
import com.hexhyperion.aklatan.utility.exception.UserNotFoundException
import io.ktor.server.config.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class ReservationService (
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val bookRepository: BookRepository,
    private val config: ApplicationConfig
) {
    suspend fun reserve(isbn: String, userId: Int): Reservation {
        return withTransaction {
            if (reservationRepository.findActiveIdByIsbnAndUserId(isbn, userId) != null) {
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
            val booksByIsbn = bookRepository.findByIsbns(isbns).associateBy { it.isbn }
            val currentReservationIsbns = reservationRepository.findActiveByIsbns(isbns)
                .filter { it.userId == userId }
                .map { it.isbn }
                .toHashSet()

            isbns.forEach { isbn ->
                if (isbn in currentReservationIsbns) {
                    throw BookAlreadyReservedException()
                }
                if (booksByIsbn[isbn] == null) {
                    throw BookNotFoundException()
                }
            }
            val reservationDays = config.property("books.reservationDurationDays").getString().toInt()
            val expiresAt = Clock.System.now() + reservationDays.days
            return@withTransaction isbns.map { reservationRepository.create(it, userId, expiresAt) }
        }
    }

    private suspend fun reservationToReadable(
        reservationRaw: Reservation,
        emailsById: Map<Int, String>? = null,
        booksByIsbn: Map<String, Book>? = null
    ): ReservationReadable {
        val userEmail = if (emailsById != null) {
            emailsById[reservationRaw.userId]
        } else {
            userRepository.findById(reservationRaw.userId)?.email
        } ?: throw UserNotFoundException()

        val book = if (booksByIsbn != null) {
            booksByIsbn[reservationRaw.isbn]
        } else {
            bookRepository.findByIsbn(reservationRaw.isbn).firstOrNull()
        } ?: throw BookNotFoundException()

        return ReservationReadable(
            id = reservationRaw.id,
            isbn = reservationRaw.isbn,
            title = book.title,
            author = book.author,
            year = book.year,
            userId = reservationRaw.userId,
            email = userEmail,
            reservedAt = reservationRaw.reservedAt,
            expiresAt = reservationRaw.expiresAt,
            canceled = reservationRaw.canceled
        )
    }

    suspend fun getReadableById(id: Int): ReservationReadable {
        val reservationRaw = reservationRepository.findById(id) ?: throw ReservationNotFoundException()
        return reservationToReadable(reservationRaw)
    }

    suspend fun getAllReadableForIsbn(isbn: String): List<ReservationReadable> {
        val reservationsRaw = reservationRepository.findByIsbn(isbn)
        val emailsById = userRepository.findAll().associate { it.id to it.email }
        val book = bookRepository.findByIsbn(isbn).firstOrNull() ?: throw BookNotFoundException()
        val booksByIsbn = mapOf(isbn to book)
        return reservationsRaw.map { reservationRaw ->
            reservationToReadable(reservationRaw, emailsById, booksByIsbn)
        }
    }

    suspend fun getAllReadableForUserId(userId: Int): List<ReservationReadable> {
        val reservationsRaw = reservationRepository.findByUserId(userId)
        val emailsById = userRepository.findAll().associate { it.id to it.email }
        return reservationsRaw.map { reservationRaw ->
            reservationToReadable(reservationRaw, emailsById)
        }
    }

    suspend fun getActiveById(id: Int): Reservation {
        return reservationRepository.findActiveById(id) ?: throw ReservationNotFoundException()
    }

    suspend fun getAllReadable(): List<ReservationReadable> {
        val reservationsRaw = reservationRepository.findAll()
        val emailsById = userRepository.findAll().associate { it.id to it.email }
        return reservationsRaw.map { reservationRaw ->
            reservationToReadable(reservationRaw, emailsById)
        }
    }

    suspend fun getExternalReservationsData(reservations: List<Reservation>): List<ExternalReservationOrBorrowData> {
        val isbns = mutableListOf<String>()
        val userIds = mutableListOf<Int>()
        reservations.forEach { reservation ->
            isbns.add(reservation.isbn)
            userIds.add(reservation.userId)
        }
        val bookNames = bookRepository.findNamesWithAuthorByIsbns(isbns)
        val userNamesAndEmails = userRepository.findNamesAndEmailsByIds(userIds)

        return bookNames.zip(userNamesAndEmails) { bookName, userNameAndEmail ->
            if (bookName == null || userNameAndEmail == null) return@zip null
            ExternalReservationOrBorrowData(userNameAndEmail.second, userNameAndEmail.first, bookName)
        }.filterNotNull()
    }

    suspend fun cancel(id: Int) {
        reservationRepository.updateCanceled(id) ?: throw ReservationNotFoundException()
    }
}
package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.api.book.BookRepository
import com.hexhyperion.aklatan.db.Borrow
import com.hexhyperion.aklatan.utility.exception.*
import io.ktor.server.config.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class BorrowService (
    private val borrowRepository: BorrowRepository,
    private val bookRepository: BookRepository,
    private val reservationRepository: ReservationRepository,
    private val config: ApplicationConfig
) {
    private suspend fun findBorrowableBookAndCurrentReservationId(isbn: String, userId: Int): Pair<Int, Int?> {
        val bookIds = bookRepository.findIdsByIsbn(isbn)
        if (bookIds.isEmpty()) {
            throw BookNotFoundException()
        }
        val currentReservationId = reservationRepository.findActiveIdByIsbnAndUserId(isbn, userId)
        val reservations = reservationRepository.findActiveOrderedByDateByIsbn(isbn)
        val blockingReservationCount = if (currentReservationId != null) {
            val priority = reservations.indexOfFirst { it.userId == userId }
            if (priority < 0) {
                throw ReservationNotFoundException()
            }
            val priorReservations = reservations.slice(0 until priority)
            priorReservations.size
        } else {
            reservations.size
        }
        val borrows = borrowRepository.findActiveByIsbn(isbn)
        val borrowedIds = borrows.map { it.bookId }
        val availableBookIds = bookIds.filter { it !in borrowedIds }
        if (availableBookIds.size <= blockingReservationCount) {
            throw NoBorrowableBooksLeftException()
        }
        return Pair(availableBookIds.first(), currentReservationId)
    }

    suspend fun borrow(isbn: String, userId: Int): Borrow {
        val (bookId, currentReservationId) = findBorrowableBookAndCurrentReservationId(isbn, userId)
        val borrowDays = config.property("books.borrowDurationDays").getString().toInt()
        val endsAt = Clock.System.now() + borrowDays.days
        val borrow = borrowRepository.create(bookId, userId, endsAt)
        if (currentReservationId != null) {
            reservationRepository.updateCanceled(currentReservationId)
        }
        return borrow
    }

    suspend fun borrowMany(isbns: Set<String>, userId: Int): List<Borrow> {
        val books = bookRepository.findByIsbns(isbns).distinctBy { it.isbn }
        if (books.size != isbns.size) {
            throw BookNotFoundException()
        }
        val bookAndReservationIds = isbns.map { isbn ->
            findBorrowableBookAndCurrentReservationId(isbn, userId)
        }
        val borrowDays = config.property("books.borrowDurationDays").getString().toInt()
        val endsAt = Clock.System.now() + borrowDays.days
        val borrows = bookAndReservationIds.map { (bookId, reservationId) ->
            val borrow = borrowRepository.create(bookId, userId, endsAt)
            if (reservationId != null) {
                reservationRepository.updateCanceled(reservationId)
            }
            borrow
        }
        return borrows
    }

    suspend fun getById(id: Int): Borrow {
        return borrowRepository.findById(id) ?: throw BorrowNotFoundException()
    }

    suspend fun getAllForBookId(bookId: Int): List<Borrow> {
        return borrowRepository.findByBookId(bookId)
    }

    suspend fun getAllForUserId(userId: Int): List<Borrow> {
        return borrowRepository.findByUserId(userId)
    }

    private suspend fun checkExtensionPossible(bookId: Int): Boolean {
        val isbn = bookRepository.findById(bookId)?.isbn ?: throw BookNotFoundException()
        val bookIds = bookRepository.findIdsByIsbn(isbn)
        val borrowedIds = borrowRepository.findActiveByIsbn(isbn).map { it.bookId }
        val availableIds = bookIds.filter { it !in borrowedIds }
        val reservationCount = reservationRepository.findActiveByIsbn(isbn).size
        return reservationCount == 0 || availableIds.size > reservationCount
    }

    suspend fun extend(id: Int): Borrow {
        val borrow = borrowRepository.findActiveById(id) ?: throw BorrowNotFoundException()
        val now = Clock.System.now()
        val isOverdue = borrow.endsAt < now
        if (!checkExtensionPossible(borrow.bookId) || isOverdue) {
            throw BorrowExtensionForbiddenException()
        }
        val extensionDays = config.property("books.borrowDurationDays").getString().toInt()
        val endsAt = now + extensionDays.days
        return borrowRepository.updateEndsAt(id, endsAt) ?: throw BorrowNotFoundException()
    }

    private fun calculateFeeForBorrow(borrow: Borrow): Double {
        val now = Clock.System.now()
        if (borrow.endsAt >= now) {
            return 0.0
        } else {
            val overdueDays = (now - borrow.endsAt).inWholeDays
            val feePerDay = config.property("books.feePerDayPln").getString().toDouble()
            return overdueDays * feePerDay
        }
    }

    suspend fun calculateReturnFee(id: Int): Double {
        val borrow = borrowRepository.findActiveById(id) ?: throw BorrowNotFoundException()
        return calculateFeeForBorrow(borrow)
    }

    suspend fun returnAndGetFee(id: Int): Double {
        val returnedAt = Clock.System.now()
        val borrow = borrowRepository.updateReturnedAt(id, returnedAt) ?: throw BorrowNotFoundException()
        return calculateFeeForBorrow(borrow)
    }
}
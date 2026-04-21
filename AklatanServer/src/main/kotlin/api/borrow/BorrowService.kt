package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.api.book.BookRepository
import com.hexhyperion.aklatan.api.user.UserRepository
import com.hexhyperion.aklatan.db.Borrow
import com.hexhyperion.aklatan.utility.exception.BookNotFoundException
import com.hexhyperion.aklatan.utility.exception.BorrowExtensionForbiddenException
import com.hexhyperion.aklatan.utility.exception.BorrowNotFoundException
import com.hexhyperion.aklatan.utility.exception.NoBooksAvailableException
import io.ktor.server.config.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class BorrowService (
    private val borrowRepository: BorrowRepository,
    private val userRepository: UserRepository,
    private val bookRepository: BookRepository,
    private val reservationRepository: ReservationRepository,
    private val config: ApplicationConfig
) {

    suspend fun borrow(isbn: String, userId: Int): Borrow {
        val bookIds = bookRepository.findIdsByIsbn(isbn)
        if (bookIds.isEmpty()) {
            throw BookNotFoundException()
        }
        val borrows = borrowRepository.findActiveByIsbn(isbn)
        val borrowedIds = borrows.map { it.bookId }
        val availableBookIds = bookIds.filter { it !in borrowedIds }
        val reservationCount = reservationRepository.findActiveByIsbn(isbn).size
        val currentReservationId = reservationRepository.findActiveIdByIsbnAndUserId(isbn, userId)
        if (availableBookIds.size <= reservationCount - if (currentReservationId != null) 1 else 0) {
            throw NoBooksAvailableException()
        }
        val bookId = availableBookIds.first()
        val borrowDays = config.property("books.borrowDurationDays").getString().toInt()
        val endsAt = Clock.System.now() + borrowDays.days
        val borrow = borrowRepository.create(bookId, userId, endsAt)
        if (currentReservationId != null) {
            reservationRepository.updateCanceled(currentReservationId)
        }
        return borrow
    }

    suspend fun getByUserId(userId: Int): List<Borrow> {
        return borrowRepository.findByUserId(userId)
    }

    suspend fun getByBookId(bookId: Int): Borrow {
        return borrowRepository.findActiveByBookId(bookId) ?: throw BorrowNotFoundException()
    }

    private suspend fun checkExtensionPossible(bookId: Int): Boolean {
        val isbn = bookRepository.findById(bookId)?.isbn ?: throw BookNotFoundException()
        val bookIds = bookRepository.findIdsByIsbn(isbn)
        val borrowedIds = borrowRepository.findActiveByIsbn(isbn).map { it.bookId }
        val availableIds = bookIds.filter { it !in borrowedIds }
        val reservationCount = reservationRepository.findActiveByIsbn(isbn).size
        return reservationCount == 0 || availableIds.size > reservationCount
    }

    suspend fun extend(bookId: Int): Borrow {
        val borrow = borrowRepository.findActiveByBookId(bookId) ?: throw BorrowNotFoundException()
        val now = Clock.System.now()
        val isOverdue = borrow.endsAt < now
        if (!checkExtensionPossible(bookId) || isOverdue) {
            throw BorrowExtensionForbiddenException()
        }
        val extensionDays = config.property("books.borrowDurationDays").getString().toInt()
        val endsAt = now + extensionDays.days
        return borrowRepository.updateEndsAt(bookId, endsAt) ?: throw BorrowNotFoundException()
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

    suspend fun calculateReturnFee(bookId: Int): Double {
        val borrow = borrowRepository.findActiveByBookId(bookId) ?: throw BorrowNotFoundException()
        return calculateFeeForBorrow(borrow)
    }

    suspend fun returnAndGetFee(bookId: Int): Double {
        val returnedAt = Clock.System.now()
        val borrow = borrowRepository.updateReturnedAt(bookId, returnedAt) ?: throw BorrowNotFoundException()
        return calculateFeeForBorrow(borrow)
    }
}
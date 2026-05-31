package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.api.book.BookRepository
import com.hexhyperion.aklatan.api.user.UserRepository
import com.hexhyperion.aklatan.db.*
import com.hexhyperion.aklatan.utility.exception.*
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
    private suspend fun findBorrowableBookAndCurrentReservationId(isbn: String, userId: Int): Pair<Int, Int?> {
        val bookIds = bookRepository.findIdsByIsbn(isbn)
        if (bookIds.isEmpty()) {
            throw BookNotFoundException()
        }
        val currentReservationId = reservationRepository.findActiveIdByIsbnAndUserId(isbn, userId)
        val reservations = reservationRepository.findActiveOrderedByDateByIsbn(isbn)
        val userIdToPriority = reservations.mapIndexed { index, reservation -> reservation.userId to index }.toMap()
        val blockingReservationCount = if (currentReservationId != null) {
            userIdToPriority[userId] ?: throw ReservationNotFoundException()
        } else {
            reservations.size
        }
        val borrows = borrowRepository.findActiveByIsbn(isbn)
        val borrowedIds = borrows.map { it.bookId }.toHashSet()
        val availableIds = bookIds.filter { it !in borrowedIds }
        if (availableIds.size <= blockingReservationCount) {
            throw NoBorrowableBooksLeftException()
        }
        return Pair(availableIds.first(), currentReservationId)
    }

    suspend fun borrow(isbn: String, userId: Int): Borrow {
        return withTransaction {
            val (bookId, currentReservationId) = findBorrowableBookAndCurrentReservationId(isbn, userId)
            val borrowDays = config.property("books.borrowDurationDays").getString().toInt()
            val endsAt = Clock.System.now() + borrowDays.days
            val borrow = borrowRepository.create(bookId, userId, endsAt)
            if (currentReservationId != null) {
                reservationRepository.updateCanceled(currentReservationId)
            }
            return@withTransaction borrow
        }
    }

    suspend fun borrowMany(isbns: Set<String>, userId: Int): List<Borrow> {
        return withTransaction {
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
                return@map borrow
            }
            return@withTransaction borrows
        }
    }

    suspend fun getById(id: Int): Borrow {
        return borrowRepository.findById(id) ?: throw BorrowNotFoundException()
    }

    private suspend fun borrowToReadable(
        borrowRaw: Borrow,
        emailsById: Map<Int, String>? = null,
        booksById: Map<Int, Book>? = null
    ): BorrowReadable {
        val userEmail = if (emailsById != null) {
            emailsById[borrowRaw.userId]
        } else {
            userRepository.findById(borrowRaw.userId)?.email
        } ?: throw UserNotFoundException()

        val book = if (booksById != null) {
            booksById[borrowRaw.bookId]
        } else {
            bookRepository.findById(borrowRaw.bookId)
        } ?: throw BookNotFoundException()

        return BorrowReadable(
            id = borrowRaw.id,
            bookId = book.id,
            isbn = book.isbn,
            title = book.title,
            author = book.author,
            year = book.year,
            userId = borrowRaw.userId,
            email = userEmail,
            borrowedAt = borrowRaw.borrowedAt,
            endsAt = borrowRaw.endsAt,
            returnedAt = borrowRaw.returnedAt,
        )
    }

    suspend fun getReadableById(id: Int): BorrowReadable {
        val borrowRaw = borrowRepository.findById(id) ?: throw BorrowNotFoundException()
        return borrowToReadable(borrowRaw)
    }

    suspend fun getAllReadableForBookId(bookId: Int): List<BorrowReadable> {
        val borrowsRaw = borrowRepository.findByBookId(bookId)
        val emailsById = userRepository.findAll().associate { it.id to it.email }
        val book = bookRepository.findById(bookId) ?: throw BookNotFoundException()
        val booksById = mapOf(bookId to book)
        return borrowsRaw.map { borrowRaw ->
            borrowToReadable(borrowRaw, emailsById, booksById)
        }
    }

    suspend fun getAllReadableForUserId(userId: Int): List<BorrowReadable> {
        val borrowsRaw = borrowRepository.findByUserId(userId)
        val emailsById = userRepository.findAll().associate { it.id to it.email }
        return borrowsRaw.map { borrowRaw ->
            borrowToReadable(borrowRaw, emailsById)
        }
    }

    suspend fun getAllReadable(): List<BorrowReadable> {
        val borrowsRaw = borrowRepository.findAll()
        val emailsById = userRepository.findAll().associate { it.id to it.email }
        return borrowsRaw.map { borrowRaw ->
            borrowToReadable(borrowRaw, emailsById)
        }
    }

    suspend fun getTotalAvailableAndReservedCountForIsbn(isbn: String): Pair<Int, Int> {
        val totalCount = bookRepository.findByIsbn(isbn).size
        if (totalCount == 0) throw BookNotFoundException()
        val borrowedCount = borrowRepository.findActiveByIsbn(isbn).size
        val reservedCount = reservationRepository.findActiveByIsbn(isbn).size
        val availableCount = totalCount - borrowedCount
        return Pair(availableCount, reservedCount)
    }

    suspend fun getAllBorrowableReservations(): List<Reservation> {
        val reservations = reservationRepository.findAllActiveOrderedByDate()
        if (reservations.isEmpty()) return emptyList()

        val isbns = reservations.map { it.isbn }.toHashSet()
        val books = bookRepository.findByIsbns(isbns)
        val totalCountByIsbn = books.groupingBy { it.isbn }.eachCount()

        val bookIdToIsbn = books.associate { it.id to it.isbn }
        val borrows = borrowRepository.findActiveByIsbns(isbns)
        val borrowedCountByIsbn = borrows
            .mapNotNull { borrow -> bookIdToIsbn[borrow.bookId] }
            .groupingBy { isbn -> isbn }
            .eachCount()

        return reservations
            .groupBy { it.isbn }
            .flatMap { (isbn, reservationsForIsbn) ->
                val totalCount = totalCountByIsbn[isbn] ?: 0
                val borrowedCount = borrowedCountByIsbn[isbn] ?: 0
                val availableCount = totalCount - borrowedCount
                if (availableCount <= 0) emptyList()
                else reservationsForIsbn.take(availableCount)
            }
    }

    suspend fun getAllEndingBorrows(): List<Borrow> {
        val notificationBeforeEndDays = config.property("books.notificationBeforeBorrowEndDays").getString().toInt()
        return borrowRepository.findEndingInDays(notificationBeforeEndDays)
    }

    suspend fun getAllOverdueBorrows(): List<Borrow> {
        return borrowRepository.findOverdue()
    }

    suspend fun getExternalBorrowsData(borrows: List<Borrow>): List<ExternalReservationOrBorrowData> {
        val bookIds = mutableListOf<Int>()
        val userIds = mutableListOf<Int>()
        borrows.forEach {
            bookIds.add(it.bookId)
            userIds.add(it.userId)
        }
        val isbns = bookRepository.findIsbnsByIds(bookIds)
        val bookNames = bookRepository.findNamesWithAuthorByIsbns(isbns.map { it ?: "" })
        val userNamesAndEmails = userRepository.findNamesAndEmailsByIds(userIds)

        return bookNames.zip(userNamesAndEmails) { bookName, userNameAndEmail ->
            if (bookName == null || userNameAndEmail == null) return@zip null
            ExternalReservationOrBorrowData(userNameAndEmail.second, userNameAndEmail.first, bookName)
        }.filterNotNull()
    }

    private suspend fun checkExtensionPossible(bookId: Int): Boolean {
        val isbn = bookRepository.findById(bookId)?.isbn ?: throw BookNotFoundException()
        val totalCount = bookRepository.findByIsbn(isbn).size
        val borrowedCount = borrowRepository.findActiveByIsbn(isbn).size
        val availableCount = totalCount - borrowedCount
        val reservedCount = reservationRepository.findActiveByIsbn(isbn).size
        return reservedCount == 0 || availableCount > reservedCount
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

    suspend fun calculateDaysLeft(id: Int): Int {
        val now = Clock.System.now()
        val borrow = borrowRepository.findById(id) ?: throw BorrowNotFoundException()
        return (borrow.endsAt - now).inWholeDays.coerceAtLeast(0).toInt()
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
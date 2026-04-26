package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.db.*
import com.hexhyperion.aklatan.utility.exception.BookNotFoundException
import com.hexhyperion.aklatan.utility.exception.UserNotFoundException
import org.jetbrains.exposed.v1.core.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class BorrowRepository {
    suspend fun create(bookId: Int, userId: Int, endsAt: Instant): Borrow = withTransaction {
        return@withTransaction BorrowEntity.new {
            this.book = BookEntity.findById(bookId) ?: throw BookNotFoundException()
            this.user = UserEntity.findById(userId) ?: throw UserNotFoundException()
            this.borrowedAt = Clock.System.now()
            this.endsAt = endsAt
        }.toBorrow()
    }

    suspend fun findById(id: Int): Borrow? = withTransaction {
        return@withTransaction BorrowEntity.findById(id)?.toBorrow()
    }

    suspend fun findByBookId(bookId: Int): List<Borrow> = withTransaction {
        return@withTransaction BorrowEntity.find { Borrows.book eq bookId }
            .map { it.toBorrow() }
    }

    suspend fun findByUserId(userId: Int): List<Borrow> = withTransaction {
        return@withTransaction BorrowEntity.find { Borrows.user eq userId }
            .map { it.toBorrow() }
    }

    suspend fun findActiveById(id: Int): Borrow? = withTransaction {
        return@withTransaction BorrowEntity.findById(id)
            ?.takeIf { it.returnedAt == null }
            ?.toBorrow()
    }

    suspend fun findActiveByIsbn(isbn: String): List<Borrow> = withTransaction {
        val bookIds = BookEntity.find { Books.isbn eq isbn }.map { it.id.value }
        return@withTransaction BorrowEntity.find {
            (Borrows.book inList bookIds) and Borrows.returnedAt.isNull()
        }.map { it.toBorrow() }
    }

    suspend fun findActiveByIsbns(isbns: Set<String>): List<Borrow> = withTransaction {
        if (isbns.isEmpty()) return@withTransaction emptyList()
        val bookIds = BookEntity.find { Books.isbn inList isbns }.map { it.id.value }
        if (bookIds.isEmpty()) return@withTransaction emptyList()
        return@withTransaction BorrowEntity.find {
            (Borrows.book inList bookIds) and Borrows.returnedAt.isNull()
        }.map { it.toBorrow() }
    }

    suspend fun findEndingInDays(days: Int): List<Borrow> = withTransaction {
        val now = Clock.System.now()
        val time = now + days.days
        return@withTransaction BorrowEntity.find {
            (Borrows.endsAt less time) and
            (Borrows.endsAt greaterEq now) and
            Borrows.returnedAt.isNull()
        }.map { it.toBorrow() }
    }

    suspend fun findOverdue(): List<Borrow> = withTransaction {
        val now = Clock.System.now()
        return@withTransaction BorrowEntity.find {
            (Borrows.endsAt less now) and
            Borrows.returnedAt.isNull()
        }.map { it.toBorrow() }
    }

    suspend fun findAll(): List<Borrow> = withTransaction {
        return@withTransaction BorrowEntity.all().map { it.toBorrow() }
    }

    suspend fun updateEndsAt(id: Int, endsAt: Instant): Borrow? = withTransaction {
        BorrowEntity.findById(id)
            ?.takeIf { it.returnedAt == null }
            ?.apply { this.endsAt = endsAt }
            ?.toBorrow()
    }

    suspend fun updateReturnedAt(id: Int, returnedAt: Instant): Borrow? = withTransaction {
        BorrowEntity.findById(id)
            ?.takeIf { it.returnedAt == null }
            ?.apply { this.returnedAt = returnedAt }
            ?.toBorrow()
    }
}
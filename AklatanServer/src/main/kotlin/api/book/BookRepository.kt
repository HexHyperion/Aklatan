package com.hexhyperion.aklatan.api.book

import com.hexhyperion.aklatan.db.Book
import com.hexhyperion.aklatan.db.BookEntity
import com.hexhyperion.aklatan.db.withTransaction

class BookRepository {
    suspend fun findAll(): List<Book> = withTransaction {
        return@withTransaction BookEntity.all()
            .map { it.toBook() }
    }
}
package com.hexhyperion.aklatan.api.book

import com.hexhyperion.aklatan.db.Book
import com.hexhyperion.aklatan.db.BookEntity
import com.hexhyperion.aklatan.db.Books
import com.hexhyperion.aklatan.db.withTransaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like

class BookRepository {
    suspend fun create(isbn: String, title: String?, author: String?, year: String?): Book = withTransaction {
        return@withTransaction BookEntity.new {
            this.isbn = isbn
            this.title = title
            this.author = author
            this.year = year
        }.toBook()
    }

    suspend fun findById(id: Int): Book? = withTransaction {
        return@withTransaction BookEntity.findById(id)?.toBook()
    }

    suspend fun findByIsbn(isbn: String): List<Book> = withTransaction {
        return@withTransaction BookEntity.find { Books.isbn eq isbn }
            .map { it.toBook() }
    }

    suspend fun findByTitle(title: String): List<Book> = withTransaction {
        return@withTransaction BookEntity.find { Books.title like "%$title%" }
            .map { it.toBook() }
    }

    suspend fun findByAuthor(author: String): List<Book> = withTransaction {
        return@withTransaction BookEntity.find { Books.author like "%$author%" }
            .map { it.toBook() }
    }

    suspend fun findAll(): List<Book> = withTransaction {
        return@withTransaction BookEntity.all()
            .map { it.toBook() }
    }

    suspend fun findIdsByIsbn(isbn: String): List<Int> = withTransaction {
        return@withTransaction BookEntity.find { Books.isbn eq isbn }
            .map { it.id.value }
    }

    suspend fun update(id: Int, isbn: String?, title: String?, author: String?, year: String?): Unit = withTransaction {
        BookEntity.findById(id)
            ?.apply {
                if (isbn != null) this.isbn = isbn
                if (title != null) this.title = title
                if (author != null) this.author = author
                if (year != null) this.year = year
            }
    }

    suspend fun delete(id: Int) = withTransaction {
        BookEntity.findById(id)?.delete()
    }
}
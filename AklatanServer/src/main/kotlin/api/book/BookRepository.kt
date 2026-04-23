package com.hexhyperion.aklatan.api.book

import com.hexhyperion.aklatan.db.Book
import com.hexhyperion.aklatan.db.BookEntity
import com.hexhyperion.aklatan.db.Books
import com.hexhyperion.aklatan.db.withTransaction
import org.jetbrains.exposed.v1.core.*

class InsensitiveLikeOp(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "ILIKE")

infix fun<T:String?> ExpressionWithColumnType<T>.ilike(pattern: T): Op<Boolean> =
    InsensitiveLikeOp(this, QueryParameter(pattern, columnType))

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

    suspend fun findByIsbns(isbns: Set<String>): List<Book> = withTransaction {
        return@withTransaction BookEntity.find { Books.isbn inList isbns }
            .map { it.toBook() }
    }

    suspend fun find(isbn: String?, titles: List<String>?, authors: List<String>?, year: String?, yearFrom: String?, yearTo: String?): List<Book> = withTransaction {
        var condition: Op<Boolean> = Books.id greaterEq 0
        if (isbn != null)
            condition = condition.and(Books.isbn eq isbn)
        if (!titles.isNullOrEmpty()) {
            val titleCondition = titles.map { Books.title ilike "%$it%" }.reduce { acc, op -> acc or op }
            condition = condition.and(titleCondition)
        }
        if (!authors.isNullOrEmpty()) {
            val authorCondition = authors.map { Books.author ilike "%$it%" }.reduce { acc, op -> acc or op }
            condition = condition.and(authorCondition)
        }
        if (year != null)
            condition = condition.and(Books.year eq year)
        if (yearFrom != null)
            condition = condition.and(Books.year greaterEq yearFrom)
        if (yearTo != null)
            condition = condition.and(Books.year lessEq yearTo)
        val query = BookEntity.find { condition }
        return@withTransaction query.map { it.toBook() }
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
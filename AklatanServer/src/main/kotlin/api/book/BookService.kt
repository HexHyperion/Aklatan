package com.hexhyperion.aklatan.api.book

import com.hexhyperion.aklatan.db.Book
import com.hexhyperion.aklatan.db.BookReadable
import com.hexhyperion.aklatan.utility.exception.BookNotFoundException

class BookService(private val bookRepository: BookRepository) {
    suspend fun add(isbn: String, title: String?, author: String?, year: String?): Book {
        return bookRepository.create(isbn, title, author, year)
    }

    suspend fun addMany(books: Set<BookWithCount>) {
        books.forEach { book ->
            repeat(book.quantity) {
                bookRepository.create(book.isbn, book.title, book.author, book.year)
            }
        }
    }

    suspend fun getById(id: Int): Book {
        return bookRepository.findById(id) ?: throw BookNotFoundException()
    }

    suspend fun getManyByIsbn(isbn: String): List<Book> {
        val books = bookRepository.findByIsbn(isbn)
        return books
    }

    suspend fun getReadableByIsbn(isbn: String): BookReadable {
        val books = bookRepository.findByIsbn(isbn)
        if (books.isEmpty()) throw BookNotFoundException()
        return books.first().toReadable()
    }

    suspend fun getAll(): List<Book> {
        return bookRepository.findAll()
    }

    suspend fun getAllReadable(): List<BookReadable> {
        return bookRepository.findAll()
            .distinctBy { it.isbn }
            .map { it.toReadable() }
    }

    suspend fun search(
        isbns: Set<String>?, titles: Set<String>?, authors: Set<String>?, years: Set<String>?, yearFrom: String?, yearTo: String?
    ): List<Book> {
        val books = bookRepository.find(isbns, titles, authors, years, yearFrom, yearTo)
        return books
    }

    suspend fun searchReadable(
        isbns: Set<String>?, titles: Set<String>?, authors: Set<String>?, years: Set<String>?, yearFrom: String?, yearTo: String?
    ): List<BookReadable> {
        val books = search(isbns, titles, authors, years, yearFrom, yearTo)
        return books.distinctBy { it.isbn }
            .map { it.toReadable() }
    }

    suspend fun editById(id: Int, isbn: String?, title: String?, author: String?, year: String?) {
        bookRepository.findById(id) ?: throw BookNotFoundException()
        bookRepository.update(id, isbn, title, author, year)
    }

    suspend fun editManyByIsbn(isbn: String, newIsbn: String?, title: String?, author: String?, year: String?) {
        val bookIds = bookRepository.findIdsByIsbn(isbn)
        if (bookIds.isEmpty()) throw BookNotFoundException()
        bookIds.forEach { id ->
            bookRepository.update(id, newIsbn, title, author, year)
        }
    }

    suspend fun remove(id: Int) {
        bookRepository.findById(id) ?: throw BookNotFoundException()
        bookRepository.delete(id)
    }

    suspend fun removeMany(ids: Set<Int>) {
        ids.forEach { id ->
            bookRepository.findById(id) ?: throw BookNotFoundException()
        }
        ids.forEach { id ->
            bookRepository.delete(id)
        }
    }
}
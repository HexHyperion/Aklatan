package com.hexhyperion.aklatan.api.book

import com.hexhyperion.aklatan.db.Book
import com.hexhyperion.aklatan.utility.exception.BookNotFoundException

class BookService(private val bookRepository: BookRepository) {
    suspend fun add(isbn: String, title: String?, author: String?, year: String?): Book {
        return bookRepository.create(isbn, title, author, year)
    }

    suspend fun addQuantity(quantity: Int, isbn: String, title: String?, author: String?, year: String?) {
        repeat(quantity) {
            bookRepository.create(isbn, title, author, year)
        }
    }

    suspend fun addMany(books: List<BookWithCount>) {
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
        if (books.isEmpty()) throw BookNotFoundException()
        return books
    }

    suspend fun getManyByTitle(title: String): List<Book> {
        val books = bookRepository.findByTitle(title)
        if (books.isEmpty()) throw BookNotFoundException()
        return books
    }

    suspend fun getManyByAuthor(author: String): List<Book> {
        val books = bookRepository.findByAuthor(author)
        if (books.isEmpty()) throw BookNotFoundException()
        return books
    }

    suspend fun search(isbn: String?, titles: List<String>?, authors: List<String>?, year: String?, yearFrom: String?, yearTo: String?): List<Book> {
        val books = bookRepository.find(isbn, titles, authors, year, yearFrom, yearTo)
        if (books.isEmpty()) throw BookNotFoundException()
        return books
    }

    suspend fun searchUnique(isbn: String?, titles: List<String>?, authors: List<String>?, year: String?, yearFrom: String?, yearTo: String?): Set<Book> {
        val books = search(isbn, titles, authors, year, yearFrom, yearTo)
        if (books.isEmpty()) throw BookNotFoundException()
        return setOf(*books.toTypedArray())
    }

    suspend fun getAll(): List<Book> {
        return bookRepository.findAll()
    }

    suspend fun getIdsByIsbn(isbn: String): List<Int> {
        val bookIds = bookRepository.findIdsByIsbn(isbn)
        if (bookIds.isEmpty()) throw BookNotFoundException()
        return bookIds
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

    suspend fun removeMany(ids: List<Int>) {
        ids.forEach { id ->
            bookRepository.findById(id) ?: throw BookNotFoundException()
        }
        ids.forEach { id ->
            bookRepository.delete(id)
        }
    }
}
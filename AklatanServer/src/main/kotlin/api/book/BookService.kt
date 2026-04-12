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

    suspend fun getByIsbn(isbn: String): List<Book> {
        return bookRepository.findByIsbn(isbn)
    }

    suspend fun getByTitle(title: String): List<Book> {
        return bookRepository.findByTitle(title)
    }

    suspend fun getByAuthor(author: String): List<Book> {
        return bookRepository.findByAuthor(author)
    }

    suspend fun getAll(): List<Book> {
        return bookRepository.findAll()
    }

    suspend fun edit(id: Int, isbn: String?, title: String?, author: String?, year: String?) {
        bookRepository.findById(id) ?: throw BookNotFoundException()
        bookRepository.update(id, isbn, title, author, year)
    }

    suspend fun remove(id: Int) {
        bookRepository.findById(id) ?: throw BookNotFoundException()
        bookRepository.delete(id)
    }
}
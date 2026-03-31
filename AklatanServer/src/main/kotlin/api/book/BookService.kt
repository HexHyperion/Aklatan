package com.hexhyperion.aklatan.api.book

import com.hexhyperion.aklatan.db.Book

class BookService(private val bookRepository: BookRepository) {
    suspend fun getAllBooks(): List<Book> {
        return bookRepository.findAll()
    }
}
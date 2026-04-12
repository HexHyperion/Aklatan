package com.hexhyperion.aklatan.api.book

import kotlinx.serialization.Serializable

@Serializable
data class BookWithCount (
    val isbn: String,
    val title: String? = null,
    val author: String? = null,
    val year: String? = null,
    val quantity: Int = 1
)

@Serializable
data class AddMultipleBooksRequest (
    val books: List<BookWithCount>
)

@Serializable
data class RemoveMultipleBooksRequest (
    val ids: List<Int>
)

@Serializable
data class EditBookRequest (
    val isbn: String? = null,
    val title: String? = null,
    val author: String? = null,
    val year: String? = null
)
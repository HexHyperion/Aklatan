package com.hexhyperion.aklatan.api.borrow

import kotlinx.serialization.Serializable

@Serializable
data class ReserveBookRequest (
    val isbn: String
)

@Serializable
data class ReserveBookAdminRequest (
    val isbn: String,
    val email: String
)

@Serializable
data class BorrowBookAdminRequest (
    val isbn: String,
    val email: String
)
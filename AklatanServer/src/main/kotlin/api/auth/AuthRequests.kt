package com.hexhyperion.api.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest (
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest (
    val email: String,
    val name: String,
    val password: String,
    val role: String?
)
package com.hexhyperion.aklatan.api.auth

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
    val role: String? = null
)

@Serializable
data class RequestEmailVerificationRequest (
    val email: String
)

@Serializable
data class VerifyEmailRequest (
    val token: String
)

@Serializable
data class RequestPasswordResetRequest (
    val email: String
)

@Serializable
data class ResetPasswordRequest (
    val token: String,
    val password: String
)
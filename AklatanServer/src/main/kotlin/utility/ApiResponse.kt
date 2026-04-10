package com.hexhyperion.aklatan.utility

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

sealed class ApiResponse<out T> {
    data class Success<T>(
        val data: T,
        val code: HttpStatusCode = HttpStatusCode.OK,
    ) : ApiResponse<T>()

    data class Error(
        val name: String,
        val message: String,
        val code: HttpStatusCode,
    ) : ApiResponse<Nothing>()
}

suspend fun <T> ApplicationCall.respond(response: ApiResponse<T>) = when (response) {
    is ApiResponse.Success -> respond(response.code, response.data as Any)
    is ApiResponse.Error -> respond(
        response.code,
        mapOf("error" to response.name, "message" to response.message)
    )
}

object ApiSuccess {
    val UserCreated = ApiResponse.Success(
        "Successfully registered, the user has been. Login now, they can.",
        HttpStatusCode.Created
    )
    val UserLoggedIn = ApiResponse.Success(
        "Successfully logged in, you have been. Access and refresh token, you now have."
    )
    val UserLoggedOut = ApiResponse.Success(
        "Successfully logged out, you have been."
    )

    val TokensRefreshed = ApiResponse.Success(
        "Both tokens refreshed, you have. Use the new ones, you should."
    )
}

object ApiError {
    val UserRoleNotFound = ApiResponse.Error(
        "USER_ROLE_NOT_FOUND",
        "Not found, the user's role was. Contact support, you should.",
        HttpStatusCode.InternalServerError
    )
    val UserAlreadyExists = ApiResponse.Error(
        "USER_ALREADY_EXISTS",
        "Exist, a user with that email already does. Use a different one, you should.",
        HttpStatusCode.Conflict
    )
    val IncorrectUserCredentials = ApiResponse.Error(
        "INCORRECT_USER_CREDENTIALS",
        "Incorrect, the e-mail or password is. Check your credentials, you should.",
        HttpStatusCode.Unauthorized
    )

    val RefreshTokenInvalid = ApiResponse.Error(
        "REFRESH_TOKEN_INVALID",
        "Invalid or expired, the refresh token is. Acquire a new one, you must.",
        HttpStatusCode.Unauthorized
    )
    val RefreshTokenNotProvided = ApiResponse.Error(
        "REFRESH_TOKEN_NOT_PROVIDED",
        "Refresh token provide, you did not.",
        HttpStatusCode.BadRequest
    )

    val UnknownError = ApiResponse.Error(
        "UNKNOWN_ERROR",
        "An unknown error, occurred has. Contact support, you should.",
        HttpStatusCode.InternalServerError
    )
}
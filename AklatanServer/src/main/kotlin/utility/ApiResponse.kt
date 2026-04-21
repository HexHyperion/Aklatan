package com.hexhyperion.aklatan.utility

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

sealed class ApiResponse<out T> {
    data class Success (
        val code: HttpStatusCode = HttpStatusCode.OK,
    ) : ApiResponse<Nothing>()

    data class SuccessWithData<T> (
        val data: T,
        val code: HttpStatusCode = HttpStatusCode.OK,
    ) : ApiResponse<T>()

    data class Error (
        val name: String,
        val message: String,
        val code: HttpStatusCode,
    ) : ApiResponse<Nothing>()
}

suspend fun <T> ApplicationCall.respond(response: ApiResponse<T>) = when (response) {
    is ApiResponse.Success -> respond(response.code)
    is ApiResponse.SuccessWithData -> respond(
        response.code,
        response.data as Any,
    )
    is ApiResponse.Error -> respond(
        response.code,
        mapOf("error" to response.name, "message" to response.message)
    )
}

object ApiError {
    val IncorrectUserCredentials = ApiResponse.Error(
        "INCORRECT_USER_CREDENTIALS",
        "Incorrect, the e-mail or password is. Check your credentials, you should.",
        HttpStatusCode.Unauthorized
    )
    val AccessTokenInvalid = ApiResponse.Error(
        "ACCESS_TOKEN_INVALID",
        "Invalid or expired, the provided access token in. Refresh it and try again, you should.",
        HttpStatusCode.Unauthorized
    )
    val DeeplinkTokenInvalid = ApiResponse.Error(
        "DEEPLINK_TOKEN_INVALID",
        "Invalid or expired, the provided token is. Request a new one, you must.",
        HttpStatusCode.Unauthorized
    )
    val RefreshTokenInvalid = ApiResponse.Error(
        "REFRESH_TOKEN_INVALID",
        "Invalid or expired, the provided refresh token is. Acquire a new one, you must.",
        HttpStatusCode.Unauthorized
    )
    val UserAlreadyExists = ApiResponse.Error(
        "USER_ALREADY_EXISTS",
        "Exist, a user with that email already does. Use a different one, you should.",
        HttpStatusCode.Conflict
    )
    val UserNotVerified = ApiResponse.Error(
        "USER_NOT_VERIFIED",
        "Not verified, your account is. Check your email for the confirmation link, you should.",
        HttpStatusCode.Unauthorized
    )

    val BookAlreadyReserved = ApiResponse.Error(
        "BOOK_ALREADY_RESERVED",
        "Already reserved by this user, the book is.",
        HttpStatusCode.Conflict
    )
    val BookAlreadyBorrowed = ApiResponse.Error(
        "BOOK_ALREADY_BORROWED",
        "Already borrowed, the book is.",
        HttpStatusCode.Conflict
    )
    val NoBooksAvailable = ApiResponse.Error(
        "NO_BOOKS_AVAILABLE",
        "No books left for borrow, there are.",
        HttpStatusCode.Conflict
    )
    val BorrowExtensionForbidden = ApiResponse.Error(
        "BORROW_EXTENSION_FORBIDDEN",
        "Forbidden, the borrow extension is. Reserved or overdue, the book might be.",
        HttpStatusCode.Conflict
    )

    val RoleNotFound = ApiResponse.Error(
        "ROLE_NOT_FOUND",
        "Exist, a role with provided name does not.",
        HttpStatusCode.NotFound
    )
    val UserNotFound = ApiResponse.Error(
        "USER_NOT_FOUND",
        "Exist, the user does not.",
        HttpStatusCode.NotFound
    )
    val BookNotFound = ApiResponse.Error(
        "BOOK_NOT_FOUND",
        "Exist, the book does not.",
        HttpStatusCode.NotFound
    )
    val ReservationNotFound = ApiResponse.Error(
        "RESERVATION_NOT_FOUND",
        "Exist, a reservation with ID provided does not.",
        HttpStatusCode.NotFound
    )
    val BorrowNotFound = ApiResponse.Error(
        "BORROW_NOT_FOUND",
        "Exist, a borrow with ID provided does not.",
        HttpStatusCode.NotFound
    )
    val WeekDayNotFound = ApiResponse.Error(
        "WEEK_DAY_NOT_FOUND",
        "Exist, the provided week day does not. Learn the calendar, you should.",
        HttpStatusCode.NotFound
    )
    val OpenHourExceptionNotFound = ApiResponse.Error(
        "OPEN_HOUR_EXCEPTION_NOT_FOUND",
        "Exist, an open hour exception on date provided does not.",
        HttpStatusCode.NotFound
    )

    val InvalidDateTimeFormat = ApiResponse.Error(
        "INVALID_DATE_TIME_FORMAT",
        "Invalid, the provided date or time format is. Check the API documentation, you should.",
        HttpStatusCode.BadRequest
    )
    val InvalidRequest = ApiResponse.Error(
        "INVALID_REQUEST",
        "Invalid, the request parameters are. Check the API documentation and try again, you should.",
        HttpStatusCode.BadRequest
    )
    val UnknownError = ApiResponse.Error(
        "UNKNOWN_ERROR",
        "An unknown error, occurred has. Contact support, you should.",
        HttpStatusCode.InternalServerError
    )
}
package com.hexhyperion.aklatan.plugins

import com.hexhyperion.aklatan.utility.ApiError
import com.hexhyperion.aklatan.utility.exception.*
import com.hexhyperion.aklatan.utility.exception.NotFoundException
import com.hexhyperion.aklatan.utility.respond
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import java.time.format.DateTimeParseException

fun Application.configureExceptionHandling() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is AuthenticationException -> {
                    when (cause) {
                        is BadCredentialsException -> {
                            call.respond(ApiError.IncorrectUserCredentials)
                        }
                        is BadAccessTokenException -> {
                            call.respond(ApiError.AccessTokenInvalid)
                        }
                        is BadDeeplinkTokenException -> {
                            call.respond(ApiError.DeeplinkTokenInvalid)
                        }
                        is BadRefreshTokenException -> {
                            call.respond(ApiError.RefreshTokenInvalid)
                        }
                        is UserExistsException -> {
                            call.respond(ApiError.UserAlreadyExists)
                        }
                        is UserNotVerifiedException -> {
                            call.respond(ApiError.UserNotVerified)
                        }
                    }
                }
                is BorrowException -> {
                    when (cause) {
                        is BookAlreadyReservedException -> {
                            call.respond(ApiError.BookAlreadyReserved)
                        }
                        is NoBorrowableBooksLeftException -> {
                            call.respond(ApiError.NoBorrowableBooksLeft)
                        }
                        is BorrowExtensionForbiddenException -> {
                            call.respond(ApiError.BorrowExtensionForbidden)
                        }
                    }
                }
                is NotFoundException -> {
                    when (cause) {
                        is RoleNotFoundException -> {
                            call.respond(ApiError.RoleNotFound)
                        }
                        is UserNotFoundException -> {
                            call.respond(ApiError.UserNotFound)
                        }
                        is BookNotFoundException -> {
                            call.respond(ApiError.BookNotFound)
                        }
                        is ReservationNotFoundException -> {
                            call.respond(ApiError.ReservationNotFound)
                        }
                        is BorrowNotFoundException -> {
                            call.respond(ApiError.BorrowNotFound)
                        }
                        is WeekDayNotFoundException -> {
                            call.respond(ApiError.WeekDayNotFound)
                        }
                        is SpecialOpenHourNotFoundException -> {
                            call.respond(ApiError.OpenHourExceptionNotFound)
                        }
                    }
                }
                is DateTimeParseException -> {
                    call.respond(ApiError.DateTimeFormatInvalid)
                }
                is ContentTransformationException -> {
                    call.respond(ApiError.InvalidRequest)
                }
                is BadRequestException -> {
                    call.respond(ApiError.InvalidRequest)
                }
                else -> {
                    cause.printStackTrace()
                    call.respond(ApiError.UnknownError)
                }
            }
        }
    }
}
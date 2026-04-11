package com.hexhyperion.aklatan.plugins

import com.hexhyperion.aklatan.utility.ApiError
import com.hexhyperion.aklatan.utility.exception.*
import com.hexhyperion.aklatan.utility.exception.NotFoundException
import com.hexhyperion.aklatan.utility.respond
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*

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
                is NotFoundException -> {
                    when (cause) {
                        is UserNotFoundException -> {
                            call.respond(ApiError.UserNotFound)
                        }
                        is RoleNotFoundException -> {
                            call.respond(ApiError.RoleNotFound)
                        }
                    }
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
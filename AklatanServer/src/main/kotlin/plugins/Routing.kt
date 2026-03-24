package com.hexhyperion.plugins

import com.hexhyperion.api.auth.RefreshTokenService
import com.hexhyperion.api.auth.authRouting
import com.hexhyperion.api.book.bookRouting
import com.hexhyperion.api.borrow.borrowRouting
import com.hexhyperion.api.user.UserService
import com.hexhyperion.api.user.userRouting
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    userService: UserService,
    refreshTokenService: RefreshTokenService
) {
    routing {
        authRouting(userService, refreshTokenService)
        userRouting()
        bookRouting()
        borrowRouting()

        authenticate("auth-jwt") {
            get("/") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val name = userService.getById(userId)?.name ?: "Unknown"

                call.respondText("Auth succeeded, as $name authenticated you are.")
            }
        }
    }
}
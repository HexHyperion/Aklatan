package com.hexhyperion.plugins

import com.hexhyperion.api.auth.authRouting
import com.hexhyperion.api.book.bookRouting
import com.hexhyperion.api.borrow.borrowRouting
import com.hexhyperion.api.user.userRouting
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        authRouting()
        userRouting()
        bookRouting()
        borrowRouting()

        authenticate("auth-jwt") {
            get("/") {
                val principal = call.principal<JWTPrincipal>()!!
                val name = principal.payload.getClaim("username").asString()

                call.respondText("Auth succeeded, as $name authenticated you are.")
            }
        }
    }
}

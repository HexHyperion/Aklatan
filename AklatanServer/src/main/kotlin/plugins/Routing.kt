package com.hexhyperion.aklatan.plugins

import com.hexhyperion.aklatan.api.admin.adminRouting
import com.hexhyperion.aklatan.api.auth.RefreshTokenService
import com.hexhyperion.aklatan.api.auth.RoleService
import com.hexhyperion.aklatan.api.auth.authRouting
import com.hexhyperion.aklatan.api.book.BookService
import com.hexhyperion.aklatan.api.book.bookRouting
import com.hexhyperion.aklatan.api.borrow.BorrowService
import com.hexhyperion.aklatan.api.borrow.ReservationService
import com.hexhyperion.aklatan.api.borrow.borrowRouting
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.api.user.userRouting
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    roleService: RoleService,
    userService: UserService,
    refreshTokenService: RefreshTokenService,
    bookService: BookService,
    reservationService: ReservationService,
    borrowService: BorrowService
) {
    routing {
        authRouting(userService, refreshTokenService)
        userRouting()
        bookRouting(userService, bookService, reservationService, borrowService)
        borrowRouting(userService, bookService, reservationService, borrowService)
        adminRouting(roleService, userService)

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
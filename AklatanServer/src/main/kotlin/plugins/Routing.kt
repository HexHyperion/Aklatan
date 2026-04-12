package com.hexhyperion.aklatan.plugins

import com.hexhyperion.aklatan.api.admin.OpenHourExceptionService
import com.hexhyperion.aklatan.api.admin.OpenHourService
import com.hexhyperion.aklatan.api.admin.adminRouting
import com.hexhyperion.aklatan.api.auth.RoleService
import com.hexhyperion.aklatan.api.auth.authRouting
import com.hexhyperion.aklatan.api.auth.tokens.PasswordResetTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RefreshTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RegistrationTokenService
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
    registrationTokenService: RegistrationTokenService,
    passwordResetTokenService: PasswordResetTokenService,
    refreshTokenService: RefreshTokenService,
    userService: UserService,
    bookService: BookService,
    reservationService: ReservationService,
    borrowService: BorrowService,
    openHourService: OpenHourService,
    openHourExceptionService: OpenHourExceptionService
) {
    routing {
        authRouting(registrationTokenService, passwordResetTokenService, refreshTokenService, userService)
        userRouting(userService)
        bookRouting(userService, bookService, reservationService, borrowService)
        borrowRouting(userService, bookService, reservationService, borrowService)
        adminRouting(
            roleService, registrationTokenService, passwordResetTokenService,
            refreshTokenService, userService, openHourService, openHourExceptionService
        )

        authenticate("auth-jwt") {
            get("/") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("id").asInt()
                val name = userService.getById(userId).name

                call.respondText("Auth succeeded, as $name authenticated you are.")
            }
        }
    }
}
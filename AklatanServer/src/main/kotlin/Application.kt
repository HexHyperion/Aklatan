package com.hexhyperion.aklatan

import com.hexhyperion.aklatan.api.admin.OpenHourRepository
import com.hexhyperion.aklatan.api.admin.OpenHourService
import com.hexhyperion.aklatan.api.admin.SpecialOpenHourRepository
import com.hexhyperion.aklatan.api.admin.SpecialOpenHourService
import com.hexhyperion.aklatan.api.auth.RoleRepository
import com.hexhyperion.aklatan.api.auth.RoleService
import com.hexhyperion.aklatan.api.auth.tokens.*
import com.hexhyperion.aklatan.api.book.BookRepository
import com.hexhyperion.aklatan.api.book.BookService
import com.hexhyperion.aklatan.api.borrow.BorrowRepository
import com.hexhyperion.aklatan.api.borrow.BorrowService
import com.hexhyperion.aklatan.api.borrow.ReservationRepository
import com.hexhyperion.aklatan.api.borrow.ReservationService
import com.hexhyperion.aklatan.api.user.UserRepository
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val roleRepository = RoleRepository()
    val roleService = RoleService(roleRepository)

    val registrationTokenRepository = RegistrationTokenRepository()
    val registrationTokenService = RegistrationTokenService(registrationTokenRepository, environment.config)
    val passwordResetTokenRepository = PasswordResetTokenRepository()
    val passwordResetTokenService = PasswordResetTokenService(passwordResetTokenRepository, environment.config)

    val refreshTokenRepository = RefreshTokenRepository()
    val refreshTokenService = RefreshTokenService(refreshTokenRepository, environment.config)

    val userRepository = UserRepository()
    val userService = UserService(userRepository, roleRepository)

    val bookRepository = BookRepository()
    val bookService = BookService(bookRepository)
    val reservationRepository = ReservationRepository()
    val reservationService = ReservationService(
        reservationRepository, userRepository, bookRepository, environment.config
    )
    val borrowRepository = BorrowRepository()
    val borrowService = BorrowService(
        borrowRepository, userRepository, bookRepository, reservationRepository, environment.config
    )

    val openHourRepository = OpenHourRepository()
    val openHourService = OpenHourService(openHourRepository)
    val specialOpenHourRepository = SpecialOpenHourRepository()
    val specialOpenHourService = SpecialOpenHourService(specialOpenHourRepository)

    configureExceptionHandling()
    configureSerialization()
    configureDatabase()
    configureAuthentication(userService, roleService)
    configureRouting(
        roleService, registrationTokenService, passwordResetTokenService, refreshTokenService, userService,
        bookService, reservationService, borrowService, openHourService, specialOpenHourService
    )
}
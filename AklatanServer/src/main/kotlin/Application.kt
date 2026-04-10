package com.hexhyperion.aklatan

import com.hexhyperion.aklatan.api.auth.RoleRepository
import com.hexhyperion.aklatan.api.auth.RoleService
import com.hexhyperion.aklatan.api.auth.tokens.RefreshTokenRepository
import com.hexhyperion.aklatan.api.auth.tokens.RefreshTokenService
import com.hexhyperion.aklatan.api.book.BookRepository
import com.hexhyperion.aklatan.api.book.BookService
import com.hexhyperion.aklatan.api.borrow.BorrowRepository
import com.hexhyperion.aklatan.api.borrow.BorrowService
import com.hexhyperion.aklatan.api.borrow.ReservationRepository
import com.hexhyperion.aklatan.api.borrow.ReservationService
import com.hexhyperion.aklatan.api.user.UserRepository
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.plugins.configureAuthentication
import com.hexhyperion.aklatan.plugins.configureDatabase
import com.hexhyperion.aklatan.plugins.configureRouting
import com.hexhyperion.aklatan.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val roleRepository = RoleRepository()
    val roleService = RoleService(roleRepository)
    val userRepository = UserRepository()
    val userService = UserService(userRepository, roleRepository)
    val refreshTokenRepository = RefreshTokenRepository()
    val refreshTokenService = RefreshTokenService(refreshTokenRepository, environment.config)
    val bookRepository = BookRepository()
    val bookService = BookService(bookRepository)
    val reservationRepository = ReservationRepository()
    val reservationService = ReservationService(reservationRepository)
    val borrowRepository = BorrowRepository()
    val borrowService = BorrowService(borrowRepository)

    configureSerialization()
    configureDatabase()
    configureAuthentication()
    configureRouting(
        roleService, userService, refreshTokenService,
        bookService, reservationService, borrowService
    )
}
package com.hexhyperion

import com.hexhyperion.api.auth.RefreshTokenRepository
import com.hexhyperion.api.auth.RefreshTokenService
import com.hexhyperion.api.auth.RoleRepository
import com.hexhyperion.api.auth.RoleService
import com.hexhyperion.api.user.UserRepository
import com.hexhyperion.api.user.UserService
import com.hexhyperion.plugins.configureAuthentication
import com.hexhyperion.plugins.configureDatabase
import com.hexhyperion.plugins.configureRouting
import com.hexhyperion.plugins.configureSerialization
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
//    val bookRepository = BookRepository()
//    val bookService = BookService(bookRepository)
//    val reservationRepository = ReservationRepository()
//    val reservationService = ReservationService(reservationRepository, bookRepository)
//    val borrowRepository = BorrowRepository()
//    val borrowService = BorrowService(borrowRepository, bookRepository)

    configureSerialization()
    configureDatabase()
    configureAuthentication()
    configureRouting(userService, refreshTokenService)
}
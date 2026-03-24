package com.hexhyperion.aklatan

import com.hexhyperion.aklatan.api.auth.RefreshTokenRepository
import com.hexhyperion.aklatan.api.auth.RefreshTokenService
import com.hexhyperion.aklatan.api.auth.RoleRepository
import com.hexhyperion.aklatan.api.auth.RoleService
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
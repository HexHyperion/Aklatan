package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.api.book.BookRepository
import com.hexhyperion.aklatan.api.user.UserRepository
import io.ktor.server.config.*

class BorrowService (
    private val borrowRepository: BorrowRepository,
    private val userRepository: UserRepository,
    private val bookRepository: BookRepository,
    private val reservationRepository: ReservationRepository,
    private val config: ApplicationConfig
) {

}
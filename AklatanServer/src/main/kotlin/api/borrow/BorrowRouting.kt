package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.api.book.BookService
import com.hexhyperion.aklatan.api.user.UserService
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.borrowRouting(
    userService: UserService,
    bookService: BookService,
    reservationService: ReservationService,
    borrowService: BorrowService
) {
    post("/reserve") {
        call.respondText("Reserve book endpoint")
    }

    post("/borrow") {
        call.respondText("Borrow book endpoint")
    }

    post("/extend") {
        call.respondText("Extend book endpoint")
    }

    post("/return") {
        call.respondText("Return book endpoint")
    }
}
package com.hexhyperion.aklatan.api.book

import com.hexhyperion.aklatan.api.borrow.BorrowService
import com.hexhyperion.aklatan.api.borrow.ReservationService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiResponse
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.bookRouting(
    userService: UserService,
    bookService: BookService,
    reservationService: ReservationService,
    borrowService: BorrowService
) {
    get("/catalog") {
        call.respondText("Catalog endpoint")
    }

    route("/inventory") {
        get {
            call.respondText("Get inventory endpoint")
        }

        post {
            call.respondText("Add to inventory endpoint")
        }

        patch {
            call.respondText("Edit inventory endpoint")
        }

        delete {
            call.respondText("Delete from inventory endpoint")
        }
    }

    get("/books") {
        val books = bookService.getAllBooks()
        call.respond(ApiResponse.SuccessWithData(books))
    }
}
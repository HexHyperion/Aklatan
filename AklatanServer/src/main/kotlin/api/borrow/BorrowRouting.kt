package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.api.book.BookService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.respond
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.borrowRouting(
    userService: UserService,
    bookService: BookService,
    reservationService: ReservationService,
    borrowService: BorrowService
) {
    authenticate("auth-jwt") {
        route("/reservations") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role == "user") {
                    val userId = principal.payload.getClaim("id").asInt()
                    val reservations = reservationService.getAllForUser(userId)
                    call.respond(reservations)
                } else {
                    val reservations = reservationService.getAll()
                    call.respond(reservations)
                }
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role == "user") {
                    val request = call.receive<ReserveBookRequest>()
                    val userId = principal.payload.getClaim("id").asInt()
                    reservationService.reserve(request.isbn, userId)
                } else {
                    val request = call.receive<ReserveBookAdminRequest>()
                    val userId = userService.getIdByEmail(request.email)
                    reservationService.reserve(request.isbn, userId)
                }
                call.respond(ApiResponse.Success())
            }

            delete("/{isbn}") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                val isbn = call.parameters["isbn"] ?: throw BadRequestException("Missing book ISBN")
                if (role == "user") {
                    val userId = principal.payload.getClaim("id").asInt()
                    val reservationId = reservationService.getActiveIdByIsbnAndUserId(isbn, userId)
                    reservationService.cancel(reservationId)
                } else {
                    val request = call.receive<CancelBookReservationAdminRequest>()
                    val userId = userService.getIdByEmail(request.email)
                    val reservationId = reservationService.getActiveIdByIsbnAndUserId(isbn, userId)
                    reservationService.cancel(reservationId)
                }
                call.respond(ApiResponse.Success())
            }
        }
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
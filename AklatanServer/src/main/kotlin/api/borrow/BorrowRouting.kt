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

            patch("/{isbn}/cancel") {
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

    route("/borrows") {
        authenticate("auth-jwt-user") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("id").asInt()
                val borrows = borrowService.getByUserId(userId)
                call.respond(ApiResponse.SuccessWithData(borrows))
            }
        }

        authenticate("auth-jwt") {
            patch("/{bookId}/extend") {
                val bookId = call.parameters["bookId"]?.toIntOrNull() ?: throw BadRequestException("Invalid book ID")
                borrowService.extend(bookId)
                call.respond(ApiResponse.Success())
            }
        }

        authenticate("auth-jwt-librarian") {
            get("/{userId}") {
                val userId = call.parameters["userId"]?.toIntOrNull() ?: throw BadRequestException("Invalid user ID")
                val borrows = borrowService.getByUserId(userId)
                call.respond(ApiResponse.SuccessWithData(borrows))
            }

            post {
                val request = call.receive<BorrowBookAdminRequest>()
                val isbn = request.isbn
                val userId = userService.getIdByEmail(request.email)
                borrowService.borrow(isbn, userId)
                call.respond(ApiResponse.Success())
            }

            route("/{bookId}") {
                get {
                    val bookId = call.parameters["bookId"]?.toIntOrNull() ?: throw BadRequestException("Invalid book ID")
                    val borrows = borrowService.getByBookId(bookId)
                    call.respond(borrows)
                }

                patch("/return") {
                    val bookId = call.parameters["bookId"]?.toIntOrNull() ?: throw BadRequestException("Invalid book ID")
                    val fee = borrowService.returnAndGetFee(bookId)
                    call.respond(ApiResponse.SuccessWithData(mapOf("fee" to fee)))
                }
            }
        }
    }
}
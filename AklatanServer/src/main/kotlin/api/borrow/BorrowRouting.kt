package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.api.book.BookService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.exception.BookNotFoundException
import com.hexhyperion.aklatan.utility.exception.BorrowNotFoundException
import com.hexhyperion.aklatan.utility.exception.ReservationNotFoundException
import com.hexhyperion.aklatan.utility.respond
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.borrowRouting(
    reservationService: ReservationService,
    userService: UserService,
    bookService: BookService,
    borrowService: BorrowService
) {
    route("/reservations") {
        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()

                val reservations = if (role == "user") {
                    val userId = principal.payload.getClaim("id").asInt()
                    reservationService.getAllReadableForUserId(userId)
                } else {
                    reservationService.getAllReadable()
                }
                call.respond(ApiResponse.SuccessWithData(reservations))
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()

                val (isbn, userId) = if (role == "user") {
                    val request = call.receive<ReserveBookRequest>()
                    Pair(request.isbn, principal.payload.getClaim("id").asInt())
                } else {
                    val request = call.receive<ReserveBookAdminRequest>()
                    Pair(request.isbn, userService.getIdByEmail(request.email))
                }
                reservationService.reserve(isbn, userId)
                call.respond(ApiResponse.Success(HttpStatusCode.Created))
            }

            post("/batch") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()

                val (isbns, userId) = if (role == "user") {
                    val request = call.receive<BatchReserveBookRequest>()
                    Pair(request.isbns, principal.payload.getClaim("id").asInt())
                } else {
                    val request = call.receive<BatchReserveBookAdminRequest>()
                    Pair(request.isbns, userService.getIdByEmail(request.email))
                }
                reservationService.reserveMany(isbns, userId)
                call.respond(ApiResponse.Success(HttpStatusCode.Created))
            }

            route("/{reservationId}") {
                get {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("id").asInt()
                    val role = principal.payload.getClaim("role").asString()
                    val reservationId = call.parameters["reservationId"]?.toIntOrNull()
                        ?: throw BadRequestException("Invalid reservation ID")

                    val reservation = reservationService.getReadableById(reservationId)
                    if (role == "user" && reservation.userId != userId) {
                        throw ReservationNotFoundException()
                    }
                    call.respond(ApiResponse.SuccessWithData(reservation))
                }

                patch("/cancel") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("id").asInt()
                    val role = principal.payload.getClaim("role").asString()
                    val reservationId = call.parameters["reservationId"]?.toIntOrNull()
                        ?: throw BadRequestException("Invalid reservation ID")

                    val reservation = reservationService.getActiveById(reservationId)
                    if (role == "user" && reservation.userId != userId) {
                        throw ReservationNotFoundException()
                    }
                    reservationService.cancel(reservationId)
                    call.respond(ApiResponse.Success())
                }
            }
        }

        authenticate("auth-jwt-librarian") {
            get("/isbn/{isbn}") {
                val isbn = call.parameters["isbn"]
                    ?: throw BadRequestException("Invalid ISBN")

                if (bookService.getManyByIsbn(isbn).isEmpty()) throw BookNotFoundException()
                val reservations = reservationService.getAllReadableForIsbn(isbn)
                call.respond(ApiResponse.SuccessWithData(reservations))
            }

            get("/user/{email}") {
                val email = call.parameters["email"]
                    ?: throw BadRequestException("Invalid user email")

                val userId = userService.getIdByEmail(email)
                val reservations = reservationService.getAllReadableForUserId(userId)
                call.respond(ApiResponse.SuccessWithData(reservations))
            }
        }
    }

    route("/borrows") {
        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()

                val borrows = if (role == "user") {
                    val userId = principal.payload.getClaim("id").asInt()
                    borrowService.getAllReadableForUserId(userId)
                } else {
                    borrowService.getAllReadable()
                }
                call.respond(ApiResponse.SuccessWithData(borrows))
            }

            route("/{borrowId}") {
                get {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("id").asInt()
                    val role = principal.payload.getClaim("role").asString()
                    val borrowId = call.parameters["borrowId"]?.toIntOrNull()
                        ?: throw BadRequestException("Invalid borrow ID")

                    val borrow = borrowService.getReadableById(borrowId)
                    if (role == "user" && borrow.userId != userId) {
                        throw BorrowNotFoundException()
                    }
                    call.respond(ApiResponse.SuccessWithData(borrow))
                }

                get("/fee") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("id").asInt()
                    val role = principal.payload.getClaim("role").asString()
                    val borrowId = call.parameters["borrowId"]?.toIntOrNull()
                        ?: throw BadRequestException("Invalid borrow ID")

                    if (role == "user") {
                        val borrow = borrowService.getById(borrowId)
                        if (borrow.userId != userId) {
                            throw BorrowNotFoundException()
                        }
                    }
                    val fee = borrowService.calculateReturnFee(borrowId)
                    call.respond(ApiResponse.SuccessWithData(mapOf("fee" to fee)))
                }

                patch("/extend") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("id").asInt()
                    val role = principal.payload.getClaim("role").asString()
                    val borrowId = call.parameters["borrowId"]?.toIntOrNull()
                        ?: throw BadRequestException("Invalid borrow ID")

                    if (role == "user") {
                        val borrow = borrowService.getById(borrowId)
                        if (borrow.userId != userId) {
                            throw BorrowNotFoundException()
                        }
                    }
                    borrowService.extend(borrowId)
                    call.respond(ApiResponse.Success())
                }
            }
        }

        authenticate("auth-jwt-librarian") {
            get("/book/{bookId}") {
                val bookId = call.parameters["bookId"]?.toIntOrNull()
                    ?: throw BadRequestException("Invalid book ID")

                bookService.getById(bookId)
                val borrows = borrowService.getAllReadableForBookId(bookId)
                call.respond(ApiResponse.SuccessWithData(borrows))
            }

            get("/user/{email}") {
                val email = call.parameters["email"]
                    ?: throw BadRequestException("Invalid user email")

                val userId = userService.getIdByEmail(email)
                val borrows = borrowService.getAllReadableForUserId(userId)
                call.respond(ApiResponse.SuccessWithData(borrows))
            }

            post {
                val request = call.receive<BorrowBookAdminRequest>()
                borrowService.borrow(request.isbn, userService.getIdByEmail(request.email))
                call.respond(ApiResponse.Success(HttpStatusCode.Created))
            }

            post("/batch") {
                val request = call.receive<BatchBorrowBookAdminRequest>()
                borrowService.borrowMany(request.isbns, userService.getIdByEmail(request.email))
                call.respond(ApiResponse.Success(HttpStatusCode.Created))
            }

            patch("/{borrowId}/return") {
                val borrowId = call.parameters["borrowId"]?.toIntOrNull()
                    ?: throw BadRequestException("Invalid borrow ID")

                val fee = borrowService.returnAndGetFee(borrowId)
                call.respond(ApiResponse.SuccessWithData(mapOf("fee" to fee)))
            }
        }
    }
}
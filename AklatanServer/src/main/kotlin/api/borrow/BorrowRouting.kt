package com.hexhyperion.aklatan.api.borrow

import com.hexhyperion.aklatan.utility.ApiResponse
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
    borrowService: BorrowService
) {
    route("/reservations") {
        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()

                val reservations = if (role == "user") {
                    val userId = principal.payload.getClaim("id").asInt()
                    reservationService.getAllForUser(userId)
                } else {
                    reservationService.getAll()
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
                    Pair(request.isbn, request.userId)
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
                    Pair(request.isbns, request.userId)
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

                    val reservation = reservationService.getById(reservationId)
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

                val reservations = reservationService.getAllActivePrioritizedForIsbn(isbn)
                call.respond(ApiResponse.SuccessWithData(reservations))
            }

            get("/user/{userId}") {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: throw BadRequestException("Invalid user ID")

                val reservations = reservationService.getAllForUser(userId)
                call.respond(ApiResponse.SuccessWithData(reservations))
            }
        }
    }

    route("/borrows") {
        authenticate("auth-jwt-user") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("id").asInt()
                val borrows = borrowService.getAllForUserId(userId)
                call.respond(ApiResponse.SuccessWithData(borrows))
            }
        }

        authenticate("auth-jwt") {
            patch("/{borrowId}/extend") {
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

        authenticate("auth-jwt-librarian") {
            get("/book/{bookId}") {
                val bookId = call.parameters["bookId"]?.toIntOrNull()
                    ?: throw BadRequestException("Invalid book ID")

                val borrows = borrowService.getAllForBookId(bookId)
                call.respond(ApiResponse.SuccessWithData(borrows))
            }

            get("/user/{userId}") {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: throw BadRequestException("Invalid user ID")

                val borrows = borrowService.getAllForUserId(userId)
                call.respond(ApiResponse.SuccessWithData(borrows))
            }

            post {
                val request = call.receive<BorrowBookAdminRequest>()
                borrowService.borrow(request.isbn, request.userId)
                call.respond(ApiResponse.Success(HttpStatusCode.Created))
            }

            post("/batch") {
                val request = call.receive<BatchBorrowBookAdminRequest>()
                borrowService.borrowMany(request.isbns, request.userId)
                call.respond(ApiResponse.Success(HttpStatusCode.Created))
            }

            route("/{borrowId}") {
                get {
                    val borrowId = call.parameters["borrowId"]?.toIntOrNull()
                        ?: throw BadRequestException("Invalid borrow ID")

                    val borrows = borrowService.getById(borrowId)
                    call.respond(ApiResponse.SuccessWithData(borrows))
                }

                get("/fee") {
                    val borrowId = call.parameters["borrowId"]?.toIntOrNull()
                        ?: throw BadRequestException("Invalid borrow ID")

                    val fee = borrowService.calculateReturnFee(borrowId)
                    call.respond(ApiResponse.SuccessWithData(mapOf("fee" to fee)))
                }

                patch("/return") {
                    val borrowId = call.parameters["borrowId"]?.toIntOrNull()
                        ?: throw BadRequestException("Invalid borrow ID")

                    val fee = borrowService.returnAndGetFee(borrowId)
                    call.respond(ApiResponse.SuccessWithData(mapOf("fee" to fee)))
                }
            }
        }
    }
}
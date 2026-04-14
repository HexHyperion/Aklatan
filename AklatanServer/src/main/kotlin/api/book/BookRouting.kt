package com.hexhyperion.aklatan.api.book

import com.hexhyperion.aklatan.api.borrow.BorrowService
import com.hexhyperion.aklatan.api.borrow.ReservationService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.respond
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.bookRouting(
    userService: UserService,
    bookService: BookService,
    reservationService: ReservationService,
    borrowService: BorrowService
) {
    route("/inventory") {
        authenticate("auth-jwt") {
            get {
                val books = bookService.getAll()
                call.respond(ApiResponse.SuccessWithData(books))
            }

            get("/search") {
                val isbn = call.request.queryParameters["isbn"]
                val title = call.request.queryParameters["title"]?.split(",")
                val author = call.request.queryParameters["author"]?.split(",")
                val year = call.request.queryParameters["year"]
                val yearFrom = call.request.queryParameters["yearFrom"]
                val yearTo = call.request.queryParameters["yearTo"]
                val books = bookService.searchUnique(isbn, title, author, year, yearFrom, yearTo)
                call.respond(ApiResponse.SuccessWithData(books))
            }
        }

        authenticate("auth-jwt-librarian") {
            post {
                val request = call.receive<AddMultipleBooksRequest>()
                bookService.addMany(request.books)
                call.respond(ApiResponse.Success(HttpStatusCode.Created))
            }

            delete {
                val request = call.receive<RemoveMultipleBooksRequest>()
                bookService.removeMany(request.ids)
                call.respond(ApiResponse.Success())
            }

            route("/id/{id}") {
                get {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("Invalid book ID")
                    val book = bookService.getById(id)
                    call.respond(ApiResponse.SuccessWithData(book))
                }

                patch {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("Invalid book ID")
                    val request = call.receive<EditBookRequest>()
                    bookService.editById(id, request.isbn, request.title, request.author, request.year)
                    call.respond(ApiResponse.Success())
                }

                delete {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("Invalid book ID")
                    bookService.remove(id)
                    call.respond(ApiResponse.Success())
                }
            }

            route("/isbn/{isbn}") {
                get {
                    val isbn = call.parameters["isbn"] ?: throw BadRequestException("Missing book ISBN")
                    val books = bookService.getManyByIsbn(isbn)
                    call.respond(ApiResponse.SuccessWithData(books))
                }

                patch {
                    val isbn = call.parameters["isbn"] ?: throw BadRequestException("Missing book ISBN")
                    val request = call.receive<EditBookRequest>()
                    bookService.editManyByIsbn(isbn, request.isbn, request.title, request.author, request.year)
                    call.respond(ApiResponse.Success())
                }
            }
        }
    }
}
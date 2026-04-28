package com.hexhyperion.aklatan.api.book

import com.hexhyperion.aklatan.api.borrow.BorrowService
import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.respond
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.bookRouting(
    bookService: BookService,
    borrowService: BorrowService
) {
    route("/inventory") {
        get("/search") {
            val isbn = call.request.queryParameters["isbn"]?.split(",")?.toHashSet()
            val title = call.request.queryParameters["title"]?.split(",")?.toHashSet()
            val author = call.request.queryParameters["author"]?.split(",")?.toHashSet()
            val year = call.request.queryParameters["year"]?.split(",")?.toHashSet()
            val yearFrom = call.request.queryParameters["yearFrom"]
            val yearTo = call.request.queryParameters["yearTo"]
            val books = bookService.searchReadable(isbn, title, author, year, yearFrom, yearTo)
            call.respond(ApiResponse.SuccessWithData(books))
        }

        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                val books = if (role == "user") {
                    bookService.getAllReadable()
                } else {
                    bookService.getAll()
                }
                call.respond(ApiResponse.SuccessWithData(books))
            }

            route("/isbn/{isbn}") {
                get {
                    val principal = call.principal<JWTPrincipal>()!!
                    val role = principal.payload.getClaim("role").asString()
                    val isbn = call.parameters["isbn"] ?: throw BadRequestException("Missing book ISBN")
                    val books = if (role == "user") {
                        bookService.getReadableByIsbn(isbn)
                    } else {
                        bookService.getManyByIsbn(isbn)
                    }
                    call.respond(ApiResponse.SuccessWithData(books))
                }

                get("/availability") {
                    val isbn = call.parameters["isbn"] ?: throw BadRequestException("Missing book ISBN")
                    val (availableCount, reservedCount) = borrowService.getTotalAvailableAndReservedCountForIsbn(isbn)
                    call.respond(ApiResponse.SuccessWithData(mapOf(
                        "available" to availableCount,
                        "reserved" to reservedCount
                    )))
                }
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

            route("/{bookId}") {
                get {
                    val bookId = call.parameters["bookId"]?.toIntOrNull() ?: throw BadRequestException("Invalid book ID")
                    val book = bookService.getById(bookId)
                    call.respond(ApiResponse.SuccessWithData(book))
                }

                patch {
                    val bookId = call.parameters["bookId"]?.toIntOrNull() ?: throw BadRequestException("Invalid book ID")
                    val request = call.receive<EditBookRequest>()
                    bookService.editById(bookId, request.isbn, request.title, request.author, request.year)
                    call.respond(ApiResponse.Success())
                }

                delete {
                    val bookId = call.parameters["bookId"]?.toIntOrNull() ?: throw BadRequestException("Invalid book ID")
                    bookService.remove(bookId)
                    call.respond(ApiResponse.Success())
                }
            }

            patch("/isbn/{isbn}") {
                val isbn = call.parameters["isbn"] ?: throw BadRequestException("Missing book ISBN")
                val request = call.receive<EditBookRequest>()
                bookService.editManyByIsbn(isbn, request.isbn, request.title, request.author, request.year)
                call.respond(ApiResponse.Success())
            }
        }
    }
}
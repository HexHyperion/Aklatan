package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.api.auth.RegisterRequest
import com.hexhyperion.aklatan.api.auth.RoleService

import com.hexhyperion.aklatan.api.user.UserService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRouting(
    roleService: RoleService,
    userService: UserService
) {
    route("/admin") {
        get("/roles") {

        }

        post("/users") {
            val librarianCredentials = call.receive<RegisterRequest>()
            if (userService.getByEmail(librarianCredentials.email) != null) {
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    "Exist, a user with that email already does. Use a different one, you should."
                )
            }

            userService.create(
                email = librarianCredentials.email,
                name = librarianCredentials.name,
                password = librarianCredentials.password,
                role = librarianCredentials.role ?: "user"
            )

            call.respond(
                HttpStatusCode.Created,
                "Successfully registered, the user has been. Login now, they can.")
        }

        post("/hours") {

        }
    }

    get("/open-hours") {

    }
}
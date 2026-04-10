package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.api.auth.RegisterRequest
import com.hexhyperion.aklatan.api.auth.RoleService

import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiError
import com.hexhyperion.aklatan.utility.ApiSuccess
import com.hexhyperion.aklatan.utility.respond
import io.ktor.server.request.*
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
                return@post call.respond(ApiError.UserAlreadyExists)
            }

            userService.create(
                email = librarianCredentials.email,
                name = librarianCredentials.name,
                password = librarianCredentials.password,
                role = librarianCredentials.role ?: "user"
            )

            call.respond(ApiSuccess.UserCreated)
        }

        post("/hours") {

        }
    }

    get("/open-hours") {

    }
}
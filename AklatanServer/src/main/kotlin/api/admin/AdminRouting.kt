package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.api.auth.RegisterRequest
import com.hexhyperion.aklatan.api.auth.RoleService
import com.hexhyperion.aklatan.api.auth.tokens.PasswordResetTokenService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.exception.UserExistsException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRouting(
    roleService: RoleService,
    passwordResetTokenService: PasswordResetTokenService,
    userService: UserService
) {
    route("/admin") {
        get("/roles") {

        }

        get("/users") {

        }

        post("/users") {
            val librarianCredentials = call.receive<RegisterRequest>()
            if (userService.checkExistsByEmail(librarianCredentials.email)) {
                throw UserExistsException()
            }

            userService.create(
                email = librarianCredentials.email,
                name = librarianCredentials.name,
                password = librarianCredentials.password,
                role = librarianCredentials.role ?: "user"
            )

            call.respond(ApiResponse.Success(HttpStatusCode.Created))
        }

        post("/open-hours") {

        }
    }

    get("/open-hours") {

    }
}
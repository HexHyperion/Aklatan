package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.api.auth.RegisterRequest
import com.hexhyperion.aklatan.api.auth.RoleService
import com.hexhyperion.aklatan.api.auth.tokens.PasswordResetTokenService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.exception.UserExistsException
import com.hexhyperion.aklatan.utility.respond
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.adminRouting(
    roleService: RoleService,
    passwordResetTokenService: PasswordResetTokenService,
    userService: UserService
) {
    authenticate("auth-jwt-manager") {
        route("/admin") {
            get("/roles") {
                val roles = roleService.getAll()
                call.respond(ApiResponse.SuccessWithData(roles))
            }

            get("/users") {
                val users = userService.getAllReadable()
                call.respond(ApiResponse.SuccessWithData(users))
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

                val userId = userService.getIdByEmail(librarianCredentials.email)
                userService.verifyEmail(userId)

                call.respond(ApiResponse.Success(HttpStatusCode.Created))
            }

            post("/open-hours") {

            }
        }
    }

    get("/open-hours") {

    }
}
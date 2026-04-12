package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.api.auth.RegisterRequest
import com.hexhyperion.aklatan.api.auth.RoleService
import com.hexhyperion.aklatan.api.auth.tokens.PasswordResetTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RefreshTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RegistrationTokenService
import com.hexhyperion.aklatan.api.user.EditAccountAdminRequest
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.exception.UserExistsException
import com.hexhyperion.aklatan.utility.exception.UserNotFoundException
import com.hexhyperion.aklatan.utility.respond
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.adminRouting(
    roleService: RoleService,
    registrationTokenService: RegistrationTokenService,
    passwordResetTokenService: PasswordResetTokenService,
    refreshTokenService: RefreshTokenService,
    userService: UserService,
    openHourService: OpenHourService,
    openHourExceptionService: OpenHourExceptionService,
) {
    authenticate("auth-jwt-manager") {
        route("/admin") {
            get("/roles") {
                val roles = roleService.getAll()
                call.respond(ApiResponse.SuccessWithData(roles))
            }

            route("/users") {
                get {
                    val users = userService.getAllReadable()
                    call.respond(ApiResponse.SuccessWithData(users))
                }

                post {
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

                route("/{id}") {
                    get {
                        val userId = call.parameters["id"]?.toIntOrNull() ?: throw UserNotFoundException()
                        val user = userService.getByIdReadable(userId)
                        call.respond(ApiResponse.SuccessWithData(user))
                    }

                    patch {
                        val userId = call.parameters["id"]?.toIntOrNull() ?: throw UserNotFoundException()
                        val request = call.receive<EditAccountAdminRequest>()
                        if (request.newName != null) {
                            userService.changeName(userId, request.newName)
                        }
                        if (request.newEmail != null) {
                            userService.changeEmail(userId, request.newEmail)
                        }
                        if (request.newPassword != null) {
                            userService.changePassword(userId, request.newPassword)
                        }
                        if (request.newRole != null) {
                            userService.changeRole(userId, request.newRole)
                        }
                        call.respond(ApiResponse.Success())
                    }

                    delete {
                        val userId = call.parameters["id"]?.toIntOrNull() ?: throw UserNotFoundException()
                        registrationTokenService.revokeAllForUser(userId)
                        passwordResetTokenService.revokeAllForUser(userId)
                        refreshTokenService.revokeAllForUser(userId)
                        userService.delete(userId)

                        call.respond(ApiResponse.Success())
                    }
                }
            }

            post("/open-hours") {

            }
        }
    }

    get("/open-hours") {

    }
}
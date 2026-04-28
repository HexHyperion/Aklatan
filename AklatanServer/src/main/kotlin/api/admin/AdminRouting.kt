package com.hexhyperion.aklatan.api.admin

import com.hexhyperion.aklatan.api.auth.RegisterRequest
import com.hexhyperion.aklatan.api.auth.RoleService
import com.hexhyperion.aklatan.api.auth.tokens.PasswordResetTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RefreshTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RegistrationTokenService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.exception.UserExistsException
import com.hexhyperion.aklatan.utility.respond
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

fun Route.adminRouting(
    roleService: RoleService,
    registrationTokenService: RegistrationTokenService,
    passwordResetTokenService: PasswordResetTokenService,
    refreshTokenService: RefreshTokenService,
    userService: UserService,
    openHourService: OpenHourService,
    specialOpenHourService: SpecialOpenHourService,
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

                    val user = userService.create(
                        email = librarianCredentials.email,
                        name = librarianCredentials.name,
                        password = librarianCredentials.password,
                        role = librarianCredentials.role ?: "user"
                    )
                    userService.verifyEmail(user.id)
                    call.respond(ApiResponse.Success(HttpStatusCode.Created))
                }

                route("/{userId}") {
                    get {
                        val userId = call.parameters["userId"]?.toIntOrNull() ?: throw BadRequestException("Invalid user ID")
                        val user = userService.getReadableById(userId)
                        call.respond(ApiResponse.SuccessWithData(user))
                    }

                    patch {
                        val userId = call.parameters["userId"]?.toIntOrNull() ?: throw BadRequestException("Invalid user ID")
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
                        if (request.verified != null) {
                            userService.verifyEmail(userId, request.verified)
                        }
                        call.respond(ApiResponse.Success())
                    }

                    delete {
                        val userId = call.parameters["userId"]?.toIntOrNull() ?: throw BadRequestException("Invalid user ID")
                        registrationTokenService.revokeAllForUser(userId)
                        passwordResetTokenService.revokeAllForUser(userId)
                        refreshTokenService.revokeAllForUser(userId)
                        userService.delete(userId)
                        call.respond(ApiResponse.Success())
                    }
                }
            }
        }

        route("/open-hours") {
            patch("/{day}") {
                val weekDay = call.parameters["day"]?.toIntOrNull() ?: throw BadRequestException("Invalid day")
                val request = call.receive<EditOpenHourRequest>()
                val openTime = request.openTime?.let { LocalTime.parse(it) }
                val closeTime = request.closeTime?.let { LocalTime.parse(it) }

                openHourService.change(weekDay, openTime, closeTime)
                call.respond(ApiResponse.Success())
            }

            route("/exceptions") {
                put("/{date}") {
                    val dateString = call.parameters["date"] ?: throw BadRequestException("Missing date parameter")
                    val date = LocalDate.parse(dateString)
                    val request = call.receive<EditSpecialOpenHourRequest>()
                    val openTime = request.openTime?.let { LocalTime.parse(it) }
                    val closeTime = request.closeTime?.let { LocalTime.parse(it) }

                    specialOpenHourService.changeOrCreate(date, openTime, closeTime, request.comment)
                    call.respond(ApiResponse.Success())
                }

                delete("/{date}") {
                    val dateString = call.parameters["date"] ?: throw BadRequestException("Missing date parameter")
                    val date = LocalDate.parse(dateString)
                    specialOpenHourService.delete(date)
                    call.respond(ApiResponse.Success())
                }
            }
        }
    }

    route("/open-hours") {
        get {
            val openHours = openHourService.getAll()
            call.respond(ApiResponse.SuccessWithData(openHours))
        }

        get("/{day}") {
            val weekDay = call.parameters["day"]?.toIntOrNull() ?: throw BadRequestException("Invalid day")
            val openHour = openHourService.getByDay(weekDay)
            call.respond(ApiResponse.SuccessWithData(openHour))
        }

        route("/exceptions") {
            get {
                val exceptions = specialOpenHourService.getAll()
                call.respond(ApiResponse.SuccessWithData(exceptions))
            }

            get("/{date}") {
                val dateString = call.parameters["date"] ?: throw BadRequestException("Missing date parameter")
                val date = LocalDate.parse(dateString)
                val specialOpenHour = specialOpenHourService.getByDate(date)
                call.respond(ApiResponse.SuccessWithData(specialOpenHour))
            }
        }
    }
}
package com.hexhyperion.aklatan.api.user

import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.respond
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.userRouting(userService: UserService) {
    authenticate("auth-jwt") {
        route("/account") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("id").asInt()
                val user = userService.getByIdReadable(userId)
                call.respond(ApiResponse.SuccessWithData(user))
            }

            patch {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("id").asInt()
                val request = call.receive<EditAccountRequest>()
                if (request.newName != null) {
                    userService.changeName(userId, request.newName)
                }
                if (request.newPassword != null) {
                    if (request.password == null) {
                        throw BadRequestException("Current password is required to change password")
                    }
                    val user = userService.getById(userId)
                    userService.authenticate(user.email, request.password)
                    userService.changePassword(userId, request.newPassword)
                }
                call.respond(ApiResponse.Success())
            }
        }
    }
}
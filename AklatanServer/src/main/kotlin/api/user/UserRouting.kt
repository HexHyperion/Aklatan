package com.hexhyperion.aklatan.api.user

import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.respond
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
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

            post {
                call.respondText("Edit account endpoint")
            }
        }
    }
}
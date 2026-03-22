package com.hexhyperion.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexhyperion.utility.getEnv
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.Date

fun Route.authRouting() {
    route("/auth") {
        authenticate("auth-basic") {
            post("/login") {
                val secret = getEnv("JWT_SECRET")
                val issuer = environment.config.property("jwt.issuer").getString()
                val audience = environment.config.property("jwt.audience").getString()
                val expirationTimeout = environment.config.property("jwt.tokenTimeout").getString().toLong()
                val expirationTime = Date(System.currentTimeMillis() + expirationTimeout)
                val user = call.principal<UserIdPrincipal>()!!

                val token = JWT.create()
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .withClaim("username", user.name)
                    .withExpiresAt(expirationTime)
                    .sign(Algorithm.HMAC256(secret))

                call.respond(token)
            }
        }

        post("/register") {
            call.respondText("Register endpoint")
        }

        post("/refresh") {
            call.respondText("Refresh token endpoint")
        }

        post("/logout") {
            call.respondText("Logout endpoint")
        }
    }
}
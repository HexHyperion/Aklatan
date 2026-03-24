package com.hexhyperion.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexhyperion.utility.getEnv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureAuthentication() {
    install(Authentication) {
        jwt("auth-jwt") {
            val issuer = this@configureAuthentication.environment.config.property("jwt.issuer").getString()
            val audience = this@configureAuthentication.environment.config.property("jwt.audience").getString()
            realm = this@configureAuthentication.environment.config.property("jwt.realm").getString()

            verifier(JWT
                .require(Algorithm.HMAC256(getEnv("JWT_SECRET")))
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
            )

            validate { credential ->
                if (credential.payload.getClaim("userId").asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Invalid or expired the access token is. Refresh it, you must."
                )
            }
        }
    }
}
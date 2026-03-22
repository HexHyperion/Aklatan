package com.hexhyperion

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexhyperion.plugins.configureDatabase
import com.hexhyperion.plugins.configureRouting
import com.hexhyperion.utility.getEnv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(Authentication) {
        basic("auth-basic") {
            realm = "Access to the '/login' endpoint"
            validate { credentials ->
                UserIdPrincipal(credentials.name)
            }
        }

        jwt("auth-jwt") {
            val issuer = this@module.environment.config.property("jwt.issuer").getString()
            val audience = this@module.environment.config.property("jwt.audience").getString()
            realm = this@module.environment.config.property("jwt.realm").getString()

            verifier(JWT
                .require(Algorithm.HMAC256(getEnv("JWT_SECRET")))
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
            )

            validate { credential ->
                if (credential.payload.getClaim("username").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Not valid the token is, or expired it has. Denied access, you are.")
            }
        }
    }

    configureDatabase()
    configureRouting()
}
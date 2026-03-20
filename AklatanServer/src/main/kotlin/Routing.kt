package com.hexhyperion

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        authenticate("auth-jwt") {
            get("/") {
                val principal = call.principal<JWTPrincipal>()!!
                val name = principal.payload.getClaim("username").asString()

                call.respondText("Auth succeeded, as $name authenticated you are.")
            }
        }

        authenticate("auth-basic") {
            post("/login") {
                val secret = getEnv("JWT_SECRET")
                val issuer = environment.config.property("jwt.issuer").getString()
                val audience = environment.config.property("jwt.audience").getString()
                val user = call.principal<UserIdPrincipal>()

                val token = JWT.create()
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .withClaim("username", user?.name)
                    .withExpiresAt(java.util.Date(System.currentTimeMillis() + 600000))
                    .sign(Algorithm.HMAC256(secret))

                call.respond(token)
            }
        }

        post("/register") {
            call.respondText("Register endpoint")
        }

        get("/account") {
            call.respondText("Account endpoint")
        }

        post("/account") {
            call.respondText("Edit account endpoint")
        }

        post("/logout") {
            call.respondText("Logout endpoint")
        }


        get("/catalog") {
            call.respondText("Catalog endpoint")
        }

        get("/inventory") {
            call.respondText("Get inventory endpoint")
        }

        post("/inventory") {
            call.respondText("Add to inventory endpoint")
        }

        patch("/inventory") {
            call.respondText("Edit inventory endpoint")
        }

        delete("/inventory") {
            call.respondText("Delete from inventory endpoint")
        }


        get("/books") {
            call.respondText("Books endpoint")
        }

        post("/reserve") {
            call.respondText("Reserve book endpoint")
        }

        post("/borrow") {
            call.respondText("Borrow book endpoint")
        }

        post("/extend") {
            call.respondText("Extend book endpoint")
        }

        post("/return") {
            call.respondText("Return book endpoint")
        }
    }
}

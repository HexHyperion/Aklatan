package com.hexhyperion

import io.ktor.server.application.*
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

fun Application.configureRouting() {
    routing {
        authenticate("auth-session") {
            get("/") {
                val userSession = call.principal<UserSession>()

                call.respondText("Auth succeeded, authenticated as ${userSession?.name}")
            }
        }

        authenticate("auth-basic") {
            post("/login") {
                val userName = call.principal<UserIdPrincipal>()?.name.toString()
                call.sessions.set(UserSession(name = userName))

                call.respondText("Login succeeded")
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

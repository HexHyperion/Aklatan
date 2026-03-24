package com.hexhyperion.aklatan.api.user

import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRouting() {
    route("/account") {
        get {
            call.respondText("Account endpoint")
        }

        post {
            call.respondText("Edit account endpoint")
        }
    }
}
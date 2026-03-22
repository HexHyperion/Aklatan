package com.hexhyperion.api.user

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

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
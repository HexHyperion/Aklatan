package com.hexhyperion.api.borrow

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.borrowRouting() {
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
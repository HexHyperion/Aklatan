package com.hexhyperion.api.book

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.bookRouting() {
    get("/catalog") {
        call.respondText("Catalog endpoint")
    }

    route("/inventory") {
        get {
            call.respondText("Get inventory endpoint")
        }

        post {
            call.respondText("Add to inventory endpoint")
        }

        patch {
            call.respondText("Edit inventory endpoint")
        }

        delete {
            call.respondText("Delete from inventory endpoint")
        }
    }

    get("/books") {
        call.respondText("Books endpoint")
    }
}
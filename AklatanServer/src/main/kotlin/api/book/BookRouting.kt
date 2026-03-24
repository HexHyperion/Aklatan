package com.hexhyperion.aklatan.api.book

import io.ktor.server.response.*
import io.ktor.server.routing.*

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
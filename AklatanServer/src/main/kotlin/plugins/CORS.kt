package com.hexhyperion.aklatan.plugins

import com.hexhyperion.aklatan.utility.Env
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import java.net.URI

fun Application.configureCORS() {
    install(CORS) {
        allowNonSimpleContentTypes = true
        allowCredentials = true

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        if (this@configureCORS.developmentMode) {
            anyHost()
        } else {
            val allowedSchemes = listOf("https")
            val allowedHosts = Env.getVar("ALLOWED_ORIGINS")
            if (allowedHosts.isNotBlank()) {
                allowedHosts.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { host ->
                    val cleanedHost = try {
                        URI.create(host).host ?: host
                    } catch (_: Exception) {
                        host
                    }
                    allowHost(cleanedHost, allowedSchemes)
                }
            }
        }

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Patch)
    }
}


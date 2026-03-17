package com.hexhyperion

import com.hexhyperion.exception.EnvFileMissingException
import com.hexhyperion.exception.EnvVariableMissingException
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import kotlinx.serialization.Serializable

var dotenv: Dotenv? = null

fun getEnv(name: String): String {
    if (dotenv == null) {
        dotenv = try {
            dotenv()
        } catch (_: Exception) {
            throw EnvFileMissingException()
        }
    }
    return dotenv?.get(name) ?: throw EnvVariableMissingException(name)
}

@Serializable
data class UserSession(
    val name: String
)

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(Sessions) {
        val secretSignKey = hex(getEnv("SESSION_SIGN_KEY"))
        header<UserSession>("X-Session-Token") {
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }
    }

    install(Authentication) {
        basic("auth-basic") {
            realm = "Aklatan"
            validate { credentials ->
                if (credentials.name == "admin" && credentials.password == "password") {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }

        session<UserSession>("auth-session") {
            validate { session ->
                if (session.name.isNotEmpty()) {
                    session
                } else {
                    null
                }
            }
            challenge {
                call.respondText("Unauthorized, user data is.")
            }
        }
    }

    configureRouting()
}

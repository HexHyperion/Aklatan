package com.hexhyperion

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexhyperion.dao.Users
import com.hexhyperion.exception.EnvFileMissingException
import com.hexhyperion.exception.EnvVariableMissingException
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

val dotenv: Dotenv by lazy {
    try {
        dotenv()
    } catch (_: Exception) {
        throw EnvFileMissingException()
    }
}

fun getEnv(name: String): String {
    return dotenv.get(name) ?: throw EnvVariableMissingException(name)
}

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

fun Application.configureDatabase() {
    val dbHost = environment.config.property("db.host").getString()
    val dbPort = environment.config.property("db.port").getString().toInt()
    val dbName = environment.config.property("db.database").getString()
    val dbUser = environment.config.property("db.username").getString()
    val dbPassword = environment.config.property("db.password").getString()

    Database.connect(
        url = "jdbc:postgresql://$dbHost:$dbPort/$dbName",
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
    )

    transaction {
        SchemaUtils.create(Users)
    }
}
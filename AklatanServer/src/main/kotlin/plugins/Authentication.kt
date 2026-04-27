package com.hexhyperion.aklatan.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexhyperion.aklatan.api.auth.RoleService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.Env
import com.hexhyperion.aklatan.utility.exception.BadAccessTokenException
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuthentication(userService: UserService, roleService: RoleService) {
    install(Authentication) {
        val issuer = this@configureAuthentication.environment.config.property("jwt.issuer").getString()
        val audience = this@configureAuthentication.environment.config.property("jwt.audience").getString()

        val jwtVerifier = JWT
            .require(Algorithm.HMAC256(Env.getVar("JWT_SECRET")))
            .withAudience(audience)
            .withIssuer(issuer)
            .build()

        suspend fun validateCredential(credential: JWTCredential, requiredRoles: List<String>? = null): JWTPrincipal? {
            val userId = credential.payload.getClaim("id").asInt()
            val user = userService.getByIdOrNull(userId) ?: return null
            val tokenRoleName = credential.payload.getClaim("role").asString()
            val serviceRoleName = roleService.getNameById(user.roleId)
            if (tokenRoleName != serviceRoleName) return null
            if (requiredRoles != null && tokenRoleName !in requiredRoles) return null
            return JWTPrincipal(credential.payload)
        }

        jwt("auth-jwt") {
            realm = "Access to general endpoints"
            verifier(jwtVerifier)
            validate { credential ->
                validateCredential(credential)
            }
            challenge { _, _ -> throw BadAccessTokenException() }
        }

        jwt("auth-jwt-user") {
            realm = "Access to user-only endpoints"
            verifier(jwtVerifier)
            validate { credential ->
                validateCredential(credential, listOf("user"))
            }
            challenge { _, _ -> throw BadAccessTokenException() }
        }

        jwt("auth-jwt-librarian") {
            realm = "Access to librarian-only endpoints"
            verifier(jwtVerifier)
            validate { credential ->
                validateCredential(credential, listOf("librarian", "manager"))
            }
            challenge { _, _ -> throw BadAccessTokenException() }
        }

        jwt("auth-jwt-manager") {
            realm = "Access to manager-only endpoints"
            verifier(jwtVerifier)
            validate { credential ->
                validateCredential(credential, listOf("manager"))
            }
            challenge { _, _ -> throw BadAccessTokenException() }
        }

        bearer("auth-internal-api-key") {
            realm = "Access to internal endpoints"
            authenticate { credential ->
                if (credential.token == Env.getVar("INTERNAL_API_KEY")) {
                    UserIdPrincipal("cron-internal-job")
                } else {
                    null
                }
            }
        }
    }
}
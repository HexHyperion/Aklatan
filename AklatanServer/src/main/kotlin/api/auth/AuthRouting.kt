package com.hexhyperion.aklatan.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.getEnv
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.authRouting(
    userService: UserService,
    refreshTokenService: RefreshTokenService,
) {
    fun generateAccessToken(userId: Int, roleName: String): String {
        val secret = getEnv("JWT_SECRET")
        val issuer = environment.config.property("jwt.issuer").getString()
        val audience = environment.config.property("jwt.audience").getString()
        val expiryMinutes = environment.config.property("jwt.accessTokenTimeoutMinutes").getString()
        val expirationTime = Date(System.currentTimeMillis() + expiryMinutes.toLong() * 1000L * 60L)

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("role", roleName)
            .withExpiresAt(expirationTime)
            .sign(Algorithm.HMAC256(secret))
    }

    route("/auth") {
        post("/login") {
            val userCredentials = call.receive<LoginRequest>()
            userService.authenticate(userCredentials.email, userCredentials.password)
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    "Incorrect, the e-mail or password is. Check your credentials, you should."
                )

            val userId = userService.getIdByEmail(userCredentials.email)!!
            val roleName = userService.getRoleNameById(userId)
                ?: return@post call.respond(
                    HttpStatusCode.InternalServerError,
                    "Not found, the user's role was. Contact support, you should."
                )

            val accessToken = generateAccessToken(userId, roleName)
            val refreshToken = refreshTokenService.generate(userId)

            call.response.cookies.append(
                Cookie(
                    name = "refreshToken",
                    value = refreshToken,
                    httpOnly = true,
                    secure = true,
                    path = "/auth/refresh"
                )
            )
            call.response.headers.append(
                HttpHeaders.Authorization,
                "Bearer $accessToken"
            )

            call.respond(
                HttpStatusCode.OK,
                "Successfully logged in, you have been. Access and refresh tokens, you now have."
            )
        }

        post("/register") {
            val userCredentials = call.receive<RegisterRequest>()
            if (userService.getByEmail(userCredentials.email) != null) {
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    "Exist, a user with that email already does. Login, or use a different one, you should."
                )
            }

            userService.create(
                email = userCredentials.email,
                name = userCredentials.name,
                password = userCredentials.password,
                role = "user"
            )

            call.respond(
                HttpStatusCode.Created,
                "Successfully registered, you have been. Login now, you can.")
        }

        post("/refresh") {
            val refreshToken = call.request.cookies["refreshToken"] ?:
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Refresh token provide, you did not."
                )

            if (refreshTokenService.validate(refreshToken)) {
                val userId = refreshTokenService.getUserId(refreshToken)!!
                val roleName = userService.getRoleNameById(userId)
                    ?: return@post call.respond(
                        HttpStatusCode.InternalServerError,
                        "Not found, the user's role was. Contact support, you should."
                    )

                refreshTokenService.revoke(refreshToken)
                val newRefreshToken = refreshTokenService.generate(userId)
                val newAccessToken = generateAccessToken(userId, roleName)

                call.response.cookies.append(
                    Cookie(
                        name = "refreshToken",
                        value = newRefreshToken,
                        httpOnly = true,
                        secure = true,
                        path = "/auth/refresh"
                    )
                )
                call.response.headers.append(
                    HttpHeaders.Authorization,
                    "Bearer $newAccessToken"
                )

                call.respond(
                    HttpStatusCode.OK,
                    "Both tokens refreshed, you have. Use the new ones, you should."
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Invalid or expired, the refresh token is. Acquire a new one, you must."
                )
            }
        }

        post("/logout") {
            val refreshToken = call.request.cookies["refreshToken"] ?:
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Refresh token provide, you did not."
                )

            if (refreshTokenService.validate(refreshToken)) {
                refreshTokenService.revoke(refreshToken)

                call.response.cookies.append(
                    Cookie(
                        name = "refreshToken",
                        value = "",
                        httpOnly = true,
                        secure = true,
                        path = "/auth/refresh",
                        maxAge = 0
                    )
                )

                call.respond(
                    HttpStatusCode.OK,
                    "Successfully logged out, you have been."
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Invalid or expired, the refresh token is. Acquire a new one, you must."
                )
            }
        }
    }
}
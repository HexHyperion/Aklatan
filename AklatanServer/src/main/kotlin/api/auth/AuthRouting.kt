package com.hexhyperion.aklatan.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexhyperion.aklatan.api.auth.tokens.RefreshTokenService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.*
import io.ktor.http.*
import io.ktor.server.request.*
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
        val expiryMinutes = environment.config.property("jwt.accessTokenTimeoutMinutes").getString().toInt()
        val expirationTime = Date(System.currentTimeMillis() + expiryMinutes * 60 * 1000)

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
                ?: return@post call.respond(ApiError.IncorrectUserCredentials)

            val userId = userService.getIdByEmail(userCredentials.email)!!
            val roleName = userService.getRoleNameById(userId)
                ?: return@post call.respond(ApiError.UserRoleNotFound)

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

            call.respond(ApiSuccess.UserLoggedIn)
        }

        post("/register") {
            val userCredentials = call.receive<RegisterRequest>()
            if (userService.getByEmail(userCredentials.email) != null) {
                return@post call.respond(ApiError.UserAlreadyExists)
            }

            userService.create(
                email = userCredentials.email,
                name = userCredentials.name,
                password = userCredentials.password,
                role = "user"
            )

            val message = EmailMessage(
                userCredentials.email,
                "Confirm your Aklatan account",
                """
                    <p>Welcome to Aklatan, ${userCredentials.name}!</p>
                    <p>Click the link below to finish creating your account and start using our services:</p>
                    <a href="#">Confirm account</a>
                    <p>If you did not create this account, you can ignore this email.</p>
                    <p>Best regards,<br>
                    Aklatan team</p>
                """.trimIndent()
            )
            message.send()

            call.respond(ApiSuccess.UserCreated)
        }

        post("/refresh") {
            val refreshToken = call.request.cookies["refreshToken"] ?:
                return@post call.respond(ApiError.RefreshTokenNotProvided)

            if (refreshTokenService.validate(refreshToken)) {
                val userId = refreshTokenService.getUserId(refreshToken)!!
                val roleName = userService.getRoleNameById(userId)
                    ?: return@post call.respond(ApiError.UserRoleNotFound)

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

                call.respond(ApiSuccess.TokensRefreshed)
            } else {
                call.respond(ApiError.RefreshTokenInvalid)
            }
        }

        post("/logout") {
            val refreshToken = call.request.cookies["refreshToken"] ?:
                return@post call.respond(ApiError.RefreshTokenNotProvided)

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

                call.respond(ApiSuccess.UserLoggedOut)
            } else {
                call.respond(ApiError.RefreshTokenInvalid)
            }
        }
    }
}
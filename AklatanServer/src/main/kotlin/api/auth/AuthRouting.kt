package com.hexhyperion.aklatan.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexhyperion.aklatan.api.auth.tokens.PasswordResetTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RefreshTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RegistrationTokenService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.util.*

fun Route.authRouting(
    registrationTokenService: RegistrationTokenService,
    passwordResetTokenService: PasswordResetTokenService,
    refreshTokenService: RefreshTokenService,
    userService: UserService,
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
            val request = call.receive<LoginRequest>()
            userService.authenticate(request.email, request.password)
                ?: return@post call.respond(ApiError.IncorrectUserCredentials)

            val userId = userService.getIdByEmail(request.email)!!
            val user = userService.getById(userId)!!

            if (user.verified) {
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
            } else {
                call.respond(ApiError.UserNotVerified)
            }
        }

        post("/register") {
            val request = call.receive<RegisterRequest>()
            if (userService.getByEmail(request.email) != null) {
                return@post call.respond(ApiError.UserAlreadyExists)
            }

            userService.create(
                email = request.email,
                name = request.name,
                password = request.password,
                role = "user"
            )

            val userId = userService.getIdByEmail(request.email)!!
            val token = registrationTokenService.generate(userId)
            val confirmationLink = "${getEnv("VERIFY_EMAIL_URL")}?token=$token"

            EmailMessage.VerifyEmailMessage(request.email, request.name, confirmationLink).send()

            call.respond(ApiSuccess.UserCreated)
        }

        post("/request-email-verification") {
            val request = call.receive<RequestEmailVerificationRequest>()
            val userId = userService.getIdByEmail(request.email)
                ?: return@post call.respond(ApiSuccess.RegistrationEmailSent)

            val user = userService.getById(userId)!!
            if (user.verified) {
                return@post call.respond(ApiSuccess.RegistrationEmailSent)
            }

            val token = registrationTokenService.generate(userId)
            val confirmationLink = "${getEnv("VERIFY_EMAIL_URL")}?token=$token"

            EmailMessage.VerifyEmailMessage(request.email, user.name, confirmationLink).send()

            call.respond(ApiSuccess.RegistrationEmailSent)
        }

        post("/verify-email") {
            val request = call.receive<VerifyEmailRequest>()
            val token = request.token

            if (registrationTokenService.validate(token)) {
                val userId = registrationTokenService.getUserId(token)!!
                registrationTokenService.revokeAllForUser(userId)
                userService.verifyEmail(userId)

                call.respond(ApiSuccess.UserVerified)
            } else {
                call.respond(ApiError.RegistrationTokenInvalid)
            }
        }

        post("/request-password-reset") {
            val request = call.receive<RequestPasswordResetRequest>()
            val userId = userService.getIdByEmail(request.email)
                ?: return@post call.respond(ApiSuccess.PasswordResetEmailSent)

            val user = userService.getById(userId)!!
            val token = passwordResetTokenService.generate(userId)
            val resetLink = "${getEnv("RESET_PASSWORD_URL")}?token=$token"

            EmailMessage.ResetPasswordMessage(request.email, user.name, resetLink).send()

            call.respond(ApiSuccess.PasswordResetEmailSent)
        }

        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            val token = request.token
            val password = request.password

            if (passwordResetTokenService.validate(token)) {
                val userId = passwordResetTokenService.getUserId(token)!!
                passwordResetTokenService.revokeAllForUser(userId)
                userService.resetPassword(userId, password)

                call.respond(ApiSuccess.PasswordReset)
            } else {
                call.respond(ApiError.PasswordResetTokenInvalid)
            }
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
            val refreshToken = call.request.cookies["refreshToken"]
                ?: return@post call.respond(ApiError.RefreshTokenNotProvided)

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
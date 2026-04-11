package com.hexhyperion.aklatan.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexhyperion.aklatan.api.auth.tokens.PasswordResetTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RefreshTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RegistrationTokenService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiSuccess
import com.hexhyperion.aklatan.utility.EmailMessage
import com.hexhyperion.aklatan.utility.exception.*
import com.hexhyperion.aklatan.utility.getEnv
import com.hexhyperion.aklatan.utility.respond
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import java.util.*

fun Route.authRouting(
    registrationTokenService: RegistrationTokenService,
    passwordResetTokenService: PasswordResetTokenService,
    refreshTokenService: RefreshTokenService,
    userService: UserService,
) {
    suspend fun sendVerificationEmail(userId: Int, email: String, name: String) {
        val token = registrationTokenService.generate(userId)
        val confirmationLink = "${getEnv("VERIFY_EMAIL_URL")}?token=$token"

        EmailMessage.VerifyEmailMessage(email, name, confirmationLink).send()
    }

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
                ?: throw BadCredentialsException()

            val userId = userService.getIdByEmail(request.email)!!
            val user = userService.getById(userId)!!

            if (user.verified) {
                val roleName = userService.getRoleNameById(userId)
                    ?: throw RoleNotFoundException()

                val accessToken = generateAccessToken(userId, roleName)
                val refreshToken = refreshTokenService.generate(userId)

                call.response.cookies.append(
                    Cookie(
                        name = "refreshToken",
                        value = refreshToken,
                        httpOnly = true,
                        secure = getEnv("ENV") == "production",
                        path = "/",
                    )
                )
                call.response.headers.append(
                    HttpHeaders.Authorization,
                    "Bearer $accessToken"
                )

                call.respond(ApiSuccess.UserLoggedIn)
            } else {
                throw UserNotVerifiedException()
            }
        }

        post("/register") {
            val request = call.receive<RegisterRequest>()
            if (userService.getByEmail(request.email) != null) {
                throw UserExistsException()
            }

            userService.create(
                email = request.email,
                name = request.name,
                password = request.password,
                role = "user"
            )

            call.application.launch { registrationTokenService.cleanupExpired() }
            call.application.launch {
                val userId = userService.getIdByEmail(request.email)!!
                sendVerificationEmail(userId, request.email, request.name)
            }

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

            call.application.launch { registrationTokenService.cleanupExpired() }
            call.application.launch { sendVerificationEmail(userId, request.email, user.name) }

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
                throw BadDeeplinkTokenException()
            }
        }

        post("/request-password-reset") {
            val request = call.receive<RequestPasswordResetRequest>()
            val userId = userService.getIdByEmail(request.email)
                ?: return@post call.respond(ApiSuccess.PasswordResetEmailSent)

            val user = userService.getById(userId)!!
            passwordResetTokenService.revokeAllForUser(userId)
            val token = passwordResetTokenService.generate(userId)
            val resetLink = "${getEnv("RESET_PASSWORD_URL")}?token=$token"

            call.application.launch { passwordResetTokenService.cleanupExpired() }
            call.application.launch { EmailMessage.ResetPasswordMessage(request.email, user.name, resetLink).send() }

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
                throw BadDeeplinkTokenException()
            }
        }

        post("/refresh") {
            call.application.launch { refreshTokenService.cleanupExpired() }

            val refreshToken = call.request.cookies["refreshToken"]
                ?: throw BadRefreshTokenException()

            if (refreshTokenService.validate(refreshToken)) {
                val userId = refreshTokenService.getUserId(refreshToken)!!
                val roleName = userService.getRoleNameById(userId)
                    ?: throw RoleNotFoundException()

                refreshTokenService.revoke(refreshToken)
                val newRefreshToken = refreshTokenService.generate(userId)
                val newAccessToken = generateAccessToken(userId, roleName)

                call.response.cookies.append(
                    Cookie(
                        name = "refreshToken",
                        value = newRefreshToken,
                        httpOnly = true,
                        secure = getEnv("ENV") == "production",
                        path = "/",
                        maxAge = 0,
                    )
                )
                call.response.headers.append(
                    HttpHeaders.Authorization,
                    "Bearer $newAccessToken"
                )

                call.respond(ApiSuccess.TokensRefreshed)
            } else {
                throw BadRefreshTokenException()
            }
        }

        post("/logout") {
            val refreshToken = call.request.cookies["refreshToken"]
                ?: throw BadRefreshTokenException()

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
                throw BadRefreshTokenException()
            }
        }
    }
}
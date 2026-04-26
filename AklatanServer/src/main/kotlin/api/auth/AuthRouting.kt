package com.hexhyperion.aklatan.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexhyperion.aklatan.api.auth.tokens.PasswordResetTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RefreshTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RegistrationTokenService
import com.hexhyperion.aklatan.api.user.UserService
import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.EmailMessage
import com.hexhyperion.aklatan.utility.Env
import com.hexhyperion.aklatan.utility.exception.BadDeeplinkTokenException
import com.hexhyperion.aklatan.utility.exception.BadRefreshTokenException
import com.hexhyperion.aklatan.utility.exception.UserExistsException
import com.hexhyperion.aklatan.utility.exception.UserNotVerifiedException
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
        val confirmationLink = "${Env.getVar("VERIFY_EMAIL_URL")}?token=$token"

        EmailMessage.VerifyEmailMessage(email, name, confirmationLink).send()
    }

    suspend fun generateAccessToken(userId: Int): String {
        val roleName = userService.getRoleNameById(userId)
        val secret = Env.getVar("JWT_SECRET")
        val issuer = environment.config.property("jwt.issuer").getString()
        val audience = environment.config.property("jwt.audience").getString()
        val expiryMinutes = environment.config.property("jwt.accessTokenTimeoutMinutes").getString().toInt()
        val expirationTime = Date(System.currentTimeMillis() + expiryMinutes * 60 * 1000)

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("id", userId)
            .withClaim("role", roleName)
            .withExpiresAt(expirationTime)
            .sign(Algorithm.HMAC256(secret))
    }

    route("/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            val user = userService.authenticate(request.email, request.password)

            if (user.verified) {
                val accessToken = generateAccessToken(user.id)
                val refreshToken = refreshTokenService.generate(user.id)

                call.response.cookies.append(
                    Cookie(
                        name = "refreshToken",
                        value = refreshToken,
                        httpOnly = true,
                        secure = Env.isProduction(),
                        path = "/",
                    )
                )
                call.respond(ApiResponse.SuccessWithData(mapOf("token" to accessToken)))
            } else {
                throw UserNotVerifiedException()
            }
        }

        post("/register") {
            val request = call.receive<RegisterRequest>()
            if (userService.checkExistsByEmail(request.email)) {
                throw UserExistsException()
            }

            val user = userService.create(
                email = request.email,
                name = request.name,
                password = request.password,
                role = "user"
            )
            call.application.launch { sendVerificationEmail(user.id, request.email, request.name) }

            call.respond(ApiResponse.Success(HttpStatusCode.Created))
        }

        post("/request-email-verification") {
            run {
                val request = call.receive<RequestEmailVerificationRequest>()
                val userId = userService.getIdByEmailOrNull(request.email) ?: return@run
                val user = userService.getById(userId)
                if (user.verified) {
                    return@run
                }
                call.application.launch { sendVerificationEmail(userId, request.email, user.name) }
            }
            call.respond(ApiResponse.Success())
        }

        post("/verify-email") {
            val request = call.receive<VerifyEmailRequest>()
            val token = request.token

            if (registrationTokenService.validate(token)) {
                val userId = registrationTokenService.getUserId(token)
                registrationTokenService.revokeAllForUser(userId)
                userService.verifyEmail(userId)
                call.respond(ApiResponse.Success())
            } else {
                throw BadDeeplinkTokenException()
            }
        }

        post("/request-password-reset") {
            run {
                val request = call.receive<RequestPasswordResetRequest>()
                val userId = userService.getIdByEmailOrNull(request.email) ?: return@run
                val user = userService.getById(userId)

                passwordResetTokenService.revokeAllForUser(userId)
                val token = passwordResetTokenService.generate(userId)
                val resetLink = "${Env.getVar("RESET_PASSWORD_URL")}?token=$token"

                call.application.launch {
                    EmailMessage.ResetPasswordMessage(request.email, user.name, resetLink).send()
                }
            }
            call.respond(ApiResponse.Success())
        }

        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            val token = request.token
            val password = request.password

            if (passwordResetTokenService.validate(token)) {
                val userId = passwordResetTokenService.getUserId(token)
                passwordResetTokenService.revokeAllForUser(userId)
                userService.changePassword(userId, password)
                call.respond(ApiResponse.Success())
            } else {
                throw BadDeeplinkTokenException()
            }
        }

        post("/refresh") {
            val refreshToken = call.request.cookies["refreshToken"] ?: throw BadRefreshTokenException()

            if (refreshTokenService.validate(refreshToken)) {
                val userId = refreshTokenService.getUserId(refreshToken)
                refreshTokenService.revoke(refreshToken)
                val newRefreshToken = refreshTokenService.generate(userId)
                val newAccessToken = generateAccessToken(userId)

                call.response.cookies.append(
                    Cookie(
                        name = "refreshToken",
                        value = newRefreshToken,
                        httpOnly = true,
                        secure = Env.isProduction(),
                        path = "/",
                        maxAge = 0,
                    )
                )
                call.respond(ApiResponse.SuccessWithData(mapOf("token" to newAccessToken)))
            } else {
                throw BadRefreshTokenException()
            }
        }

        post("/logout") {
            val refreshToken = call.request.cookies["refreshToken"] ?: throw BadRefreshTokenException()

            if (refreshTokenService.validate(refreshToken)) {
                refreshTokenService.revoke(refreshToken)

                call.response.cookies.append(
                    Cookie(
                        name = "refreshToken",
                        value = "",
                        httpOnly = true,
                        secure = Env.isProduction(),
                        path = "/auth/refresh",
                        maxAge = 0
                    )
                )
                call.respond(ApiResponse.Success())
            } else {
                throw BadRefreshTokenException()
            }
        }
    }
}
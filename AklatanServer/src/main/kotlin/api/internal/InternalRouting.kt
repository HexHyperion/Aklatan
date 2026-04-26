package com.hexhyperion.aklatan.api.internal

import com.hexhyperion.aklatan.api.auth.tokens.PasswordResetTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RefreshTokenService
import com.hexhyperion.aklatan.api.auth.tokens.RegistrationTokenService
import com.hexhyperion.aklatan.api.borrow.BorrowService
import com.hexhyperion.aklatan.api.borrow.ReservationService
import com.hexhyperion.aklatan.utility.ApiResponse
import com.hexhyperion.aklatan.utility.EmailMessage
import com.hexhyperion.aklatan.utility.respond
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

fun Route.internalRouting(
    registrationTokenService: RegistrationTokenService,
    passwordResetTokenService: PasswordResetTokenService,
    refreshTokenService: RefreshTokenService,
    reservationService: ReservationService,
    borrowService: BorrowService,
) {
    authenticate("auth-internal-api-key") {
        route("/internal") {
            post("/send-email-notifications") {
                val dateFormat = LocalDateTime.Format {
                    year(); char('-'); this@Format.monthNumber(padding = Padding.ZERO); char('-'); this@Format.day(padding = Padding.ZERO)
                    char(' '); hour(); char(':'); minute()
                }

                call.application.launch {
                    val borrowableReservations = borrowService.getAllBorrowableReservations()
                    val reservationData = reservationService.getExternalReservationsData(borrowableReservations)
                    for ((reservation, data) in borrowableReservations.zip(reservationData)) {
                        EmailMessage.ReservedBookAvailableMessage(
                            data.userEmail, data.userName, data.bookName,
                            reservation.expiresAt
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .format(dateFormat)
                        ).send()
                    }
                }

                call.application.launch {
                    val endingBorrows = borrowService.getAllEndingBorrows()
                    val borrowData = borrowService.getExternalBorrowsData(endingBorrows)
                    for ((borrow, data) in endingBorrows.zip(borrowData)) {
                        EmailMessage.BookBorrowEndingMessage(
                            data.userEmail, data.userName, data.bookName,
                            borrowService.calculateDaysLeft(borrow.id).toString()
                        ).send()
                    }
                }

                call.application.launch {
                    val overdueBorrows = borrowService.getAllOverdueBorrows()
                    val borrowData = borrowService.getExternalBorrowsData(overdueBorrows)
                    for ((borrow, data) in overdueBorrows.zip(borrowData)) {
                        EmailMessage.BookOverdueMessage(
                            data.userEmail, data.userName, data.bookName,
                            borrowService.calculateReturnFee(borrow.id).toString()
                        ).send()
                    }
                }

                call.respond(ApiResponse.Success())
            }

            post("/cleanup-expired-tokens") {
                call.application.launch { registrationTokenService.cleanupExpired() }
                call.application.launch { passwordResetTokenService.cleanupExpired() }
                call.application.launch { refreshTokenService.cleanupExpired() }
                call.respond(ApiResponse.Success())
            }
        }
    }
}
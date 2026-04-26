package com.hexhyperion.aklatan.utility

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

open class EmailMessage (
    val to: String,
    val subject: String,
    val body: String,
) {
    suspend fun send() = withContext(Dispatchers.IO) {
        val properties = Properties()
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"
        properties["mail.smtp.host"] = Env.getVar("SMTP_HOST")
        properties["mail.smtp.port"] = Env.getVar("SMTP_PORT")

        val username = Env.getVar("SMTP_USERNAME")
        val password = Env.getVar("SMTP_PASSWORD")

        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })

        val message = MimeMessage(session)
        message.setFrom(InternetAddress(username))
        message.addRecipient(Message.RecipientType.TO, InternetAddress(to))
        message.subject = subject
        message.setContent(body, "text/html; charset=utf-8")
        Transport.send(message)
    }

    data class VerifyEmailMessage (
        val email: String,
        val name: String,
        val confirmationLink: String,
    ) : EmailMessage(
        to = email,
        subject = "Verify your Aklatan account",
        body = """
            <p>Welcome to Aklatan, $name!</p>
            <p>Click the link below to finish creating your account and start using our library services:</p>
            <a href="$confirmationLink">Confirm account</a>
            <p>If you did not create this account, you can ignore this email.</p>
            <p>May the words be with you,<br>
            Aklatan team</p>
        """.trimIndent()
    )

    data class ResetPasswordMessage (
        val email: String,
        val name: String,
        val resetLink: String,
    ) : EmailMessage(
        to = email,
        subject = "Reset your Aklatan password",
        body = """
            <p>Hello there, $name!</p>
            <p>Click the link below to reset your Aklatan password:</p>
            <a href="$resetLink">Reset your password</a>
            <p>If you did not request a password change, you can safely ignore this email.</p>
            <p>May the words be with you,<br>
            Aklatan team</p>
        """.trimIndent()
    )

    data class ReservedBookAvailableMessage (
        val email: String,
        val name: String,
        val bookName: String,
        val reservationEndDate: String,
    ) : EmailMessage(
        to = email,
        subject = "Your reserved book is now available!",
        body = """
            <p>Hello there, $name!</p>
            <p>We're happy to inform that your reserved book, <i>$bookName</i>, is now available in our library! Come before your reservation ends ($reservationEndDate) to pick it up.</p>
            <p>May the words be with you,<br>
            Aklatan team</p>
        """.trimIndent()
    )

    data class BookBorrowEndingMessage (
        val email: String,
        val name: String,
        val bookName: String,
        val daysLeft: String,
    ) : EmailMessage(
        to = email,
        subject = "Your borrow is about to end!",
        body = """
            <p>Hello there, $name!</p>
            <p>Your borrow for the book <i>$bookName</i> ends in $daysLeft days. Please return the book on time to avoid fees, or extend the borrow if you're still reading.</p>
            <p>May the words be with you,<br>
            Aklatan team</p>
        """.trimIndent()
    )

    data class BookOverdueMessage (
        val email: String,
        val name: String,
        val bookName: String,
        val fee: String,
    ) : EmailMessage(
        to = email,
        subject = "Your book is overdue!",
        body = """
            <p>Hello there, $name!</p>
            <p>Your borrow for the book <i>$bookName</i> has ended! Your overdue penalty is currently $fee PLN. Please return the book as soon as possible to avoid further costs.</p>
            <p>May the words be with you,<br>
            Aklatan team</p>
        """.trimIndent()
    )
}
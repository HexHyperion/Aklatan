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
            <p>To finish creating your account and start using our library services, the link below you should click:</p>
            <a href="$confirmationLink">Confirm account</a>
            <p>If create this account you did not, ignore this email, you can.</p>
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
            <p>To reset your Aklatan password, the link below you should click:</p>
            <a href="$resetLink">Reset your password</a>
            <p>If request a password change you did not, safely ignore this email, you can.</p>
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
        subject = "Available, your reserved book now is!",
        body = """
            <p>Hello there, $name!</p>
            <p>Happy to inform we are, that your reserved book, <i>$bookName</i>, now available in our library is! Come and borrow it before your reservation ends on $reservationEndDate, you can.</p>
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
        subject = "About to end, your borrow is!",
        body = """
            <p>Hello there, $name!</p>
            <p>End in $daysLeft days, your borrow for the book <i>$bookName</i> does. To avoid fees, return the book on time, you must, or extend the borrow, if still reading you are.</p>
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
        subject = "Overdue, your book is!",
        body = """
            <p>Hello there, $name!</p>
            <p>Ended, your borrow for the book <i>$bookName</i> has! $fee PLN, your current overdue penalty is. To avoid further costs, return the book as soon as possible, you must.</p>
            <p>May the words be with you,<br>
            Aklatan team</p>
        """.trimIndent()
    )
}
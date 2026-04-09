package com.hexhyperion.aklatan.utility

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

data class EmailMessage (
    val to: String,
    val subject: String,
    val body: String,
) {
    suspend fun send() = withContext(Dispatchers.IO) {
        val properties = Properties()
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"
        properties["mail.smtp.host"] = getEnv("SMTP_HOST")
        properties["mail.smtp.port"] = getEnv("SMTP_PORT")

        val username = getEnv("SMTP_USERNAME")
        val password = getEnv("SMTP_PASSWORD")

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
}
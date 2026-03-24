package com.hexhyperion.aklatan.plugins

import com.hexhyperion.aklatan.db.Users
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureDatabase() {
    val dbHost = environment.config.property("db.host").getString()
    val dbPort = environment.config.property("db.port").getString().toInt()
    val dbName = environment.config.property("db.database").getString()
    val dbUser = environment.config.property("db.username").getString()
    val dbPassword = environment.config.property("db.password").getString()

    Database.connect(
        url = "jdbc:postgresql://$dbHost:$dbPort/$dbName",
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
    )

    transaction {
        SchemaUtils.create(Users)
    }
}
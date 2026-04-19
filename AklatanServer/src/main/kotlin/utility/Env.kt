package com.hexhyperion.aklatan.utility

import com.hexhyperion.aklatan.utility.exception.EnvFileMissingException
import com.hexhyperion.aklatan.utility.exception.EnvVariableMissingException
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv

val dotenv: Dotenv by lazy {
    try {
        dotenv()
    } catch (_: Exception) {
        throw EnvFileMissingException()
    }
}

fun getEnv(name: String): String {
    return dotenv.get(name) ?: throw EnvVariableMissingException(name)
}

fun isProduction(): Boolean {
    return dotenv.get("ENV") == "production"
}
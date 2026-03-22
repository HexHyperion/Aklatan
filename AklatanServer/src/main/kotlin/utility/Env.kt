package com.hexhyperion.utility

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

open class EnvException(message: String?) : Exception(message)

class EnvFileMissingException(
    message: String? = "Failed to load the .env file. Ensure it exists in the project root and is properly formatted."
) : EnvException(message)

class EnvVariableMissingException(
    variableName: String,
    message: String? = "Required environment variable '$variableName' is missing. Please add it to the .env file."
) : EnvException(message)
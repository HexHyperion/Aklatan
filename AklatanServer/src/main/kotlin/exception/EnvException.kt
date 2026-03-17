package com.hexhyperion.exception

open class EnvException(message: String?) : Exception(message)

class EnvFileMissingException(
    message: String? = "Failed to load the .env file. Ensure it exists in the project root and is properly formatted."
) : EnvException(message)

class EnvVariableMissingException(
    variableName: String,
    message: String? = "Required environment variable '$variableName' is missing. Please add it to the .env file."
) : EnvException(message)
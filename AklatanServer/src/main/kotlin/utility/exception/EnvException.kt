package com.hexhyperion.aklatan.utility.exception

open class EnvException(message: String?) : Exception(message)

class EnvVariableMissingException(
    variableName: String,
    message: String? = "Required environment variable '$variableName' is missing. Please add it to the .env file."
) : EnvException(message)
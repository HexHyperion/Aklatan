package com.hexhyperion.aklatan.utility.exception

open class EnvException(message: String?) : Exception(message)

class EnvVariableMissingException(
    variableName: String,
    message: String? = "Missing, the required environment variable '$variableName' is. Add it to the .env file, you must."
) : EnvException(message)
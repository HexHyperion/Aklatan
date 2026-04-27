package com.hexhyperion.aklatan.utility

import com.hexhyperion.aklatan.utility.exception.EnvVariableMissingException

class Env {
    companion object {
        fun getVar(name: String): String {
            return System.getenv(name) ?: throw EnvVariableMissingException(name)
        }
    }
}
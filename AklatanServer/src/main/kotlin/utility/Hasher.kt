package com.hexhyperion.aklatan.utility

import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class Hasher {
    companion object {
        fun sha256Hash(str: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(str.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        fun bcryptHash(str: String): String {
            return BCrypt.hashpw(str, BCrypt.gensalt())
        }

        fun bcryptVerify(str: String, hash: String): Boolean {
            return BCrypt.checkpw(str, hash)
        }

        fun generateRandomString(tokenLength: Int): String {
            val secureRandom = SecureRandom()
            val bytes = ByteArray(tokenLength)
            secureRandom.nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
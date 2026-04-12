package com.hexhyperion.aklatan.api.user

import kotlinx.serialization.Serializable

@Serializable
data class EditAccountRequest (
    val newName: String? = null,
    val password: String? = null,
    val newPassword: String? = null
)
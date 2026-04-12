package com.hexhyperion.aklatan.api.user

import kotlinx.serialization.Serializable

@Serializable
data class EditAccountRequest (
    val newName: String? = null,
    val password: String? = null,
    val newPassword: String? = null
)

@Serializable
data class EditAccountAdminRequest (
    val newName: String? = null,
    val newEmail: String? = null,
    val newPassword: String? = null,
    val newRole: String? = null
)
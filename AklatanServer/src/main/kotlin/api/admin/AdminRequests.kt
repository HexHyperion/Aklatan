package com.hexhyperion.aklatan.api.admin

import kotlinx.serialization.Serializable

@Serializable
data class EditAccountAdminRequest (
    val newName: String? = null,
    val newEmail: String? = null,
    val newPassword: String? = null,
    val newRole: String? = null,
    val verified: Boolean? = null
)

@Serializable
data class EditOpenHoursRequest (
    val openTime: String? = null,
    val closeTime: String? = null
)

@Serializable
data class EditSpecialOpenHourRequest (
    val openTime: String? = null,
    val closeTime: String? = null,
    val comment: String? = null
)
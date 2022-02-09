package dev.brella.magicauth

import kotlinx.serialization.Serializable

@Serializable
data class TOTPAccount(
    val algorithm: String,
    val periodMilliseconds: Long,
    val digits: Int,
    val accountName: String,
    val issuer: String?,
    val iconUrl: String? = null,
    val tokenName: String
)
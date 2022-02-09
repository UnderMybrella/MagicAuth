package dev.brella.magicauth

import java.security.spec.KeySpec
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

data class TOTPSecretKey(private val algorithm: String, private val encoded: ByteArray): KeySpec, SecretKey {
    override fun getAlgorithm(): String = algorithm
    override fun getFormat(): String = "RAW"
    override fun getEncoded(): ByteArray = encoded
}
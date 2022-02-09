package dev.brella.magicauth

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.apache.commons.codec.binary.Base32
import java.io.File
import java.io.InputStream
import javax.crypto.Mac

class MagicAuth : Application(), CoroutineScope by MainScope() {
    companion object {
        const val SLEEP_MS = 250L
        val BASE32 = Base32()

        inline fun decodeBase32(str: String) =
            BASE32.decode(str)

        inline fun encodeBase32(data: ByteArray) =
            BASE32.encodeToString(data)
    }

    var timeRemaining by mutableStateOf(0)
    var timeProgress by mutableStateOf(0f)
    var token by mutableStateOf("")

    var counterValue by mutableStateOf(-1L)
    var selectedAccount by mutableStateOf(-1)
    val accounts = mutableStateListOf<TOTPAccount>()

    // = Mac.getInstance("HmacSHA1")
    //        .apply { init(SecretKeySpec(MainActivity.BASE32.decode("WAVL3MIB22U34B26"), "HmacSHA1")) }

    val tokenJob = launch {
        var start: Long? = null
        while (isActive) {
            val config = accounts.getOrNull(selectedAccount)
            if (config == null) {
                delay(SLEEP_MS)
                continue
            }
            if (start == null) start =
                System.currentTimeMillis() - (System.currentTimeMillis() % config.periodMilliseconds)

            val currentTime = System.currentTimeMillis() - start

            timeRemaining = (currentTime % config.periodMilliseconds).toInt()
            timeProgress = currentTime / config.periodMilliseconds.toFloat()

            val curCounter = currentTime / config.periodMilliseconds
            if (curCounter != counterValue) {
                counterValue = curCounter

                launch inner@{
                    // HOTP value = HOTP(K, C) mod 10d
                    // HOTP(K, C) = truncate(HMACH(K, C))
                    // truncate(MAC) = extract31(MAC, MAC[(19 × 8) + 4:(19 × 8) + 7])
                    // extract31(MAC, i) = MAC[i × 8 + 1:i × 8 + (4 × 8) − 1]

                    val mac = Mac.getInstance(config.algorithm)
                    val secret = readTokenFor(config) ?: return@inner
                    mac.init(TOTPSecretKey(config.algorithm, secret))

                    val hmac = mac.doFinal(
                        byteArrayOf(
                            (curCounter shr 56).toByte(),
                            (curCounter shr 48).toByte(),
                            (curCounter shr 40).toByte(),
                            (curCounter shr 32).toByte(),
                            (curCounter shr 24).toByte(),
                            (curCounter shr 16).toByte(),
                            (curCounter shr 8).toByte(),
                            (curCounter shr 0).toByte(),
                        )
                    )

                    secret.fill(0)

                    val lsb = (hmac.last().toInt() and 0x0F)
                    val truncate = ((hmac[lsb].toInt() and 0x7F) shl 24) or
                            ((hmac[lsb + 1].toInt() and 0xFF) shl 16) or
                            ((hmac[lsb + 2].toInt() and 0xFF) shl 8) or
                            ((hmac[lsb + 3].toInt() and 0xFF) shl 0)

                    token = truncate.toString()
                        .takeLast(config.digits) //truncate % 1_000_000).toString()
                }
            }

            delay(SLEEP_MS - (currentTime % SLEEP_MS))
        }
    }


    internal fun writeTokenFor(token: ByteArray, account: TOTPAccount) {
        // Creates a file with this name, or replaces an existing file
        // that has the same name. Note that the file name cannot contain
        // path separators.
        val fileToWrite = File(applicationContext.filesDir, "${account.tokenName}.dat")

        val mainKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            applicationContext,
            fileToWrite,
            mainKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

// File cannot exist before using openFileOutput
        if (fileToWrite.exists()) {
            fileToWrite.delete()
        }

        encryptedFile.openFileOutput().use { out -> out.write(token) }
    }


    internal fun readTokenFor(account: TOTPAccount): ByteArray? {
        val fileToRead =
            File(applicationContext.filesDir, "${account.tokenName}.dat")
        if (!fileToRead.exists()) return null

        val mainKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            applicationContext,
            fileToRead,
            mainKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return encryptedFile.openFileInput().use(InputStream::readBytes)
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal fun readAccounts() {
        val fileToRead = File(applicationContext.filesDir, "totp_accounts.json")
        if (!fileToRead.exists()) return

        val mainKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            applicationContext,
            fileToRead,
            mainKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        var accounts: List<TOTPAccount> = emptyList()

        for (i in 0 until 8) {
            try {
                accounts = encryptedFile.openFileInput().use(Json::decodeFromStream)
                break
            } catch (e: SerializationException) {
                e.printStackTrace()

                Toast.makeText(this, e.stackTraceToString(), Toast.LENGTH_SHORT)
                    .show()
            }
        }

        this.accounts.clear()
        this.accounts.addAll(accounts)
    }
    @OptIn(ExperimentalSerializationApi::class)
    internal fun writeAccounts() {
        val fileToWrite = File(applicationContext.filesDir, "totp_accounts.json")

        val mainKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            applicationContext,
            fileToWrite,
            mainKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

// File cannot exist before using openFileOutput
        if (fileToWrite.exists()) {
            fileToWrite.delete()
        }

        encryptedFile.openFileOutput().use { out -> Json.encodeToStream(accounts as List<TOTPAccount>, out) }
    }

    override fun onCreate() {
        super.onCreate()

        readAccounts()
    }
}
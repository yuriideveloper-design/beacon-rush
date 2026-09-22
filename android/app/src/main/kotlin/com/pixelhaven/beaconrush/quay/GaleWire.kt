package com.pixelhaven.beaconrush.quay

import android.content.Context
import android.util.Base64
import android.webkit.WebSettings
import com.pixelhaven.beaconrush.BeaconRushApp
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GaleWire(context: Context) {
    private val browseAgent = runCatching {
        WebSettings.getDefaultUserAgent(context.applicationContext)
    }.getOrDefault(FALLBACK_UA).replace(HULL_TICKS, "")

    private val client = HttpClient(Android) {
        expectSuccess = false
        install(UserAgent) { agent = browseAgent }
        install(HttpTimeout) {
            connectTimeoutMillis = 12_000
            requestTimeoutMillis = 12_000
            socketTimeoutMillis = 12_000
        }
        engine {
            connectTimeout = 12_000
            socketTimeout = 12_000
        }
    }

    suspend fun castRush(bag: JSONObject): Pair<Int, String>? = withContext(Dispatchers.IO) {
        val href = LampCask.unwind("k0") + LampCask.unwind("k1")
        val method = LampCask.unwind("k6")
        val h0 = LampCask.unwind("k4")
        val h1 = LampCask.unwind("k5")
        val sealed = sealBody(bag.toString(), LampCask.unwind("k7"))
        FogChalk.i("call $method $href ua=${browseAgent.take(48)}")
        return@withContext try {
            val resp = client.request(href) {
                this.method = when (method.lowercase()) {
                    "put" -> HttpMethod.Put
                    "patch" -> HttpMethod.Patch
                    else -> HttpMethod.Post
                }
                header(h0, bag.optString(CorkSack.H_UID).take(12))
                header(h1, bag.optString(CorkSack.H_BLD).take(8))
                contentType(ContentType.Text.Plain)
                setBody(sealed)
            }
            val text = resp.bodyAsText()
            FogChalk.i("call status=${resp.status.value} bodyLen=${text.length}")
            resp.status.value to text
        } catch (e: Exception) {
            FogChalk.e("call failed $method $href", e)
            null
        }
    }

    suspend fun reportPulse(id: String, opened: Boolean) = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext
        val tail = if (opened) LampCask.unwind("k3") else LampCask.unwind("k2")
        val href = LampCask.unwind("k0") + tail.replace("{id}", id)
        try {
            client.request(href) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
        } catch (e: Exception) {
            FogChalk.w("pulse failed", e)
        }
    }

    private fun sealBody(plain: String, passphrase: String): String {
        val key = MessageDigest.getInstance("SHA-256").digest(passphrase.toByteArray(Charsets.UTF_8))
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val enc = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + enc, Base64.NO_WRAP)
    }

    companion object {
        private val HULL_TICKS = Regex("""; wv| Version/4\.0""")
        private const val FALLBACK_UA =
            "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        fun from(context: Context): GaleWire {
            val app = context.applicationContext as? BeaconRushApp
            return app?.galeWire ?: GaleWire(context.applicationContext)
        }
    }
}

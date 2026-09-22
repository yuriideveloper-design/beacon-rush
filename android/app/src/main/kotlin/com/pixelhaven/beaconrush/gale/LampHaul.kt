package com.pixelhaven.beaconrush.gale

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.core.net.toUri
import java.io.File

class LampHaul(
    private val context: Context,
    private val webViewProvider: () -> android.webkit.WebView?,
) {
    fun shouldLeaveWebView(uri: Uri): Boolean {
        val raw = uri.toString()
        if (raw.startsWith("data:image/", ignoreCase = true)) return true
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme == "http" || scheme == "https") {
            return isMessengerHttpsHost(uri.host?.lowercase())
        }
        if (scheme in INTERNAL_SCHEMES) return false
        return true
    }

    fun route(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        val url = uri.toString()

        if (url.startsWith("data:image/", ignoreCase = true)) {
            return shareDataImage(url)
        }

        if (scheme == "tg" || host == "t.me" || host == "telegram.me") {
            var target = uri
            if (scheme != "tg") {
                val path = uri.path
                val username = if (path != null && path.length > 1) path.substring(1) else null
                if (!username.isNullOrEmpty() && !username.contains("/")) {
                    target = "tg://resolve?domain=$username".toUri()
                }
            }
            if (tryLaunch(target, "org.telegram.messenger") ||
                tryLaunch(target, "org.telegram.messenger.web")
            ) {
                return true
            }
            openPlayStore("org.telegram.messenger")
            return true
        }

        if (scheme == "whatsapp" || host == "wa.me" || host.endsWith("whatsapp.com")) {
            var target = uri
            if (scheme != "whatsapp" && host == "wa.me") {
                val path = uri.path
                val phone = if (path != null && path.length > 1) path.substring(1) else ""
                val text = uri.getQueryParameter("text")
                var waUrl = "whatsapp://send?phone=$phone"
                if (!text.isNullOrEmpty()) waUrl += "&text=" + Uri.encode(text)
                target = waUrl.toUri()
            }
            if (tryLaunch(target, "com.whatsapp") || tryLaunch(target, "com.whatsapp.w4b")) {
                return true
            }
            openPlayStore("com.whatsapp")
            return true
        }

        if (scheme == "viber" || host.endsWith("viber.com")) {
            if (tryLaunch(uri, "com.viber.voip")) return true
            openPlayStore("com.viber.voip")
            return true
        }

        if (scheme == "instagram" || host.contains("instagram.com")) {
            var target = uri
            if (scheme != "instagram") {
                val path = uri.path
                val username = if (path != null && path.length > 1) path.substring(1) else null
                if (!username.isNullOrEmpty() && !username.contains("/")) {
                    target = "instagram://user?username=$username".toUri()
                }
            }
            if (tryLaunch(target, "com.instagram.android")) return true
            openPlayStore("com.instagram.android")
            return true
        }

        if (scheme == "tiktok" || host.contains("tiktok.com")) {
            if (tryLaunch(uri, "com.zhiliaoapp.musically") ||
                tryLaunch(uri, "com.ss.android.ugc.trill")
            ) {
                return true
            }
            openPlayStore("com.zhiliaoapp.musically")
            return true
        }

        if (scheme == "diia" || host.contains("diia.app") || host.contains("diia.gov.ua")) {
            if (tryLaunch(uri, "ua.gov.diia.app")) return true
            openPlayStore("ua.gov.diia.app")
            return true
        }

        if (scheme == "tel") {
            return try {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }

        if (scheme == "mailto") {
            return try {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }

        if (url.startsWith("market://")) {
            return try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }

        if (url.startsWith("intent://") || scheme == "intent") {
            return handleIntentUrl(url)
        }

        return openOrTell(uri)
    }

    private fun handleIntentUrl(url: String): Boolean {
        return try {
            val parsed = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                component = null
                selector = null
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(parsed)
                return true
            } catch (_: Exception) {
            }
            parsed.data?.let { if (openOrTell(it)) return true }
            val fallback = parsed.getStringExtra("browser_fallback_url")
            if (!fallback.isNullOrEmpty()) {
                webViewProvider()?.loadUrl(fallback)
                return true
            }
            true
        } catch (_: Exception) {
            true
        }
    }

    private fun openOrTell(uri: Uri): Boolean {
        if (tryOpenView(uri)) return true
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme.isBlank() || scheme in INTERNAL_SCHEMES) return false
        val storePackage = storeHint(scheme)
        if (storePackage != null) {
            openPlayStore(storePackage)
        } else {
            tellUnavailable()
        }
        return true
    }

    private fun storeHint(scheme: String): String? = when (scheme) {
        "tg" -> "org.telegram.messenger"
        "whatsapp" -> "com.whatsapp"
        "viber" -> "com.viber.voip"
        "diia" -> "ua.gov.diia.app"
        "instagram" -> "com.instagram.android"
        "bhim" -> "in.org.npci.upiapp"
        "upi" -> "com.google.android.apps.nbu.paisa.user"
        "gpay" -> "com.google.android.apps.nbu.paisa.user"
        "paytm" -> "net.one97.paytm"
        "phonepe" -> "com.phonepe.app"
        else -> null
    }

    private fun tryOpenView(uri: Uri): Boolean {
        val plain = Intent(Intent.ACTION_VIEW, uri).apply {
            component = null
            selector = null
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(plain)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun tryLaunch(uri: Uri, packageName: String): Boolean {
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun openPlayStore(packageName: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$packageName".toUri(),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: ActivityNotFoundException) {
            }
        }
    }

    private fun shareDataImage(url: String): Boolean {
        return try {
            val comma = url.indexOf(',')
            if (comma < 0) return false
            val meta = url.substring(0, comma)
            val payload = url.substring(comma + 1)
            val mime = meta.removePrefix("data:").substringBefore(";").ifBlank { "image/png" }
            val bytes = if (meta.contains("base64", ignoreCase = true)) {
                Base64.decode(payload, Base64.DEFAULT)
            } else {
                Uri.decode(payload).toByteArray()
            }
            val ext = when {
                mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
                mime.contains("webp") -> "webp"
                mime.contains("gif") -> "gif"
                else -> "png"
            }
            val dir = CrateNub.exportDir(context)
            val file = File(dir, "share_${System.currentTimeMillis()}.$ext")
            file.writeBytes(bytes)
            val uri = CrateNub.uriForFile(context, file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (_: Exception) {
            tellUnavailable()
            true
        }
    }

    private fun tellUnavailable() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, "App not available", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private val INTERNAL_SCHEMES = setOf(
            "https", "about", "data", "http", "file", "blob", "javascript",
        )

        private fun isMessengerHttpsHost(host: String?): Boolean {
            if (host.isNullOrEmpty()) return false
            return host.contains("instagram.com") ||
                host == "wa.me" || host.endsWith("whatsapp.com") ||
                host.contains("tiktok.com") || host == "t.me" ||
                host == "telegram.me" || host.endsWith("viber.com") ||
                host.contains("diia.gov.ua") || host.contains("diia.app")
        }
    }
}

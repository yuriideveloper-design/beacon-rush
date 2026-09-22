package com.pixelhaven.beaconrush.gale

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Base64
import android.webkit.WebView
import java.io.File
import org.json.JSONObject

class DuneUrn(context: Context) {
    private val store = File(context.applicationContext.filesDir, STORE)

    fun hasStateFor(target: String): Boolean {
        if (target.isBlank() || !store.exists()) return false
        val json = runCatching { JSONObject(store.readText()) }.getOrNull() ?: return false
        return json.optString(KEY_DEST) == target && json.optString(KEY_STATE).isNotBlank()
    }

    fun save(webView: WebView, target: String) {
        if (target.isBlank()) return
        val bundle = Bundle()
        webView.saveState(bundle)
        val encoded = marshal(bundle) ?: run {
            clear()
            return
        }
        store.writeText(
            JSONObject()
                .put(KEY_STATE, encoded)
                .put(KEY_DEST, target)
                .put(KEY_LAST_URL, webView.url ?: "")
                .toString(),
        )
    }

    fun restoreInto(webView: WebView, target: String): Boolean {
        if (!hasStateFor(target)) return false
        val json = runCatching { JSONObject(store.readText()) }.getOrNull() ?: return false
        val encoded = json.optString(KEY_STATE)
        val bundle = unmarshal(encoded) ?: run {
            clear()
            return false
        }
        return try {
            webView.restoreState(bundle) != null
        } catch (_: Exception) {
            clear()
            false
        }
    }

    fun clear() {
        if (store.exists()) store.delete()
    }

    private fun marshal(bundle: Bundle): String? {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(bundle)
            Base64.encodeToString(parcel.marshall(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        } finally {
            parcel.recycle()
        }
    }

    private fun unmarshal(encoded: String): Bundle? {
        val parcel = Parcel.obtain()
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            parcel.readBundle(WebView::class.java.classLoader)
        } catch (_: Exception) {
            null
        } finally {
            parcel.recycle()
        }
    }

    private companion object {
        const val STORE = "beaconrush12_surface"
        const val KEY_STATE = "state_b64"
        const val KEY_DEST = "tgt"
        const val KEY_LAST_URL = "last_href"
    }
}

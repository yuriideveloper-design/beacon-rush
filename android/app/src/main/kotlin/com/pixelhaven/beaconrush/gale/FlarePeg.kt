package com.pixelhaven.beaconrush.gale

import android.content.Context
import android.webkit.JavascriptInterface

@androidx.annotation.Keep
class FlarePeg(private val context: Context) {
    @androidx.annotation.Keep
    @JavascriptInterface
    fun putd5(b64: String, name: String, mime: String) {
        val dataUrl = "data:${mime.ifBlank { "application/octet-stream" }};base64,$b64"
        FetchCork.saveFromJs(context, dataUrl, mime)
    }

    @androidx.annotation.Keep
    @JavascriptInterface
    fun pingf0(dataUrl: String, mime: String) {
        FetchCork.saveFromJs(context, dataUrl, mime)
    }
}

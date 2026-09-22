package com.pixelhaven.beaconrush.gale

import android.webkit.CookieManager
import android.webkit.WebView

object WaxPad {
    fun bind(view: WebView) {
        runCatching {
            val jar = CookieManager.getInstance()
            jar.setAcceptCookie(true)
            jar.setAcceptThirdPartyCookies(view, true)
            jar.flush()
        }
    }

    fun seal() {
        runCatching { CookieManager.getInstance().flush() }
    }
}

package com.pixelhaven.beaconrush.buoy

import android.content.Context
import java.io.File
import org.json.JSONObject

class BuoyClip(
    context: Context,
) {
    private val file = File(context.applicationContext.filesDir, STORE)

    data class BuoyPin(
        val href: String,
        val expiresAtEpochMs: Long,
    )

    fun pinClip(href: String, expiresAtEpochMs: Long) {
        file.writeText(
            JSONObject()
                .put(K_HREF, href)
                .put(K_HOLD, true)
                .put(K_UNTIL, expiresAtEpochMs)
                .toString(),
        )
    }

    fun peekClip(nowMs: Long = System.currentTimeMillis()): BuoyPin? {
        val json = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return null
        if (!json.optBoolean(K_HOLD, false)) return null
        val href = json.optString(K_HREF).takeIf { DashCast.isLiveHref(it) } ?: return null
        val until = json.optLong(K_UNTIL, 0L)
        if (until in 1..<nowMs) {
            dropClip()
            return null
        }
        return BuoyPin(href, until)
    }

    fun sameClip(href: String): Boolean {
        val held = peekClip() ?: return false
        return held.href == href
    }

    fun dropClip() {
        if (file.exists()) file.delete()
    }

    private companion object {
        const val STORE = "beaconrush12_surface.hold"
        const val K_HREF = "target"
        const val K_HOLD = "keep"
        const val K_UNTIL = "until"
    }
}

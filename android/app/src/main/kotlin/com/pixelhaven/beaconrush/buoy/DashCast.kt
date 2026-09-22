package com.pixelhaven.beaconrush.buoy

import com.pixelhaven.beaconrush.quay.FogChalk
import org.json.JSONObject

object DashCast {
    const val STATE = "phase"
    const val HREF = "target"
    const val STICKY = "keep"
    const val TTL = "until"
    const val PASS = "pe7"
    const val OFF = "n48"

    fun parseDash(raw: String, latch: BuoyClip): QuayLane {
        if (raw.isBlank()) {
            FogChalk.i("pick blank body → white unless clip")
            return latch.peekClip()?.toLane() ?: QuayLane.BlankDesk
        }
        val json = runCatching { JSONObject(raw) }.getOrNull()
        if (json == null) {
            FogChalk.i("pick not JSON")
            return latch.peekClip()?.toLane() ?: QuayLane.BlankDesk
        }
        val state = json.optString(STATE)
        val href = json.optString(HREF).trim()
        val keep = json.optString(STICKY)
        val untilRaw = json.optString(TTL)
        FogChalk.i("pick phase=$state target=$href keep=$keep until=$untilRaw")
        if (state != PASS || !isLiveHref(href)) {
            FogChalk.i("pick white need phase=$PASS + http(s)")
            latch.dropClip()
            return QuayLane.BlankDesk
        }
        val expires = untilRaw.toLongOrNull() ?: 0L
        val sticky = keep == PASS
        if (sticky) {
            latch.pinClip(href, expires)
        } else {
            latch.dropClip()
        }
        return QuayLane.OpenLamp(
            href = href,
            restoreHistory = latch.sameClip(href),
            persistOnPause = sticky,
        )
    }

    fun isLiveHref(href: String): Boolean =
        href.startsWith("https://") || href.startsWith("http://")

    private fun BuoyClip.BuoyPin.toLane() = QuayLane.OpenLamp(
        href = href,
        restoreHistory = true,
        persistOnPause = true,
    )
}

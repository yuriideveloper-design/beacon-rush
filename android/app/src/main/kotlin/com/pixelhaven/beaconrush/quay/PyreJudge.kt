package com.pixelhaven.beaconrush.quay

import com.pixelhaven.beaconrush.buoy.BuoyClip
import com.pixelhaven.beaconrush.buoy.DashCast
import com.pixelhaven.beaconrush.buoy.QuayLane
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

class PyreJudge(
    private val bag: CorkSack,
    private val hop: GaleWire,
    private val latch: BuoyClip,
) {
    suspend fun sortQuay(minWarmupMs: Long = WARMUP_MS): QuayLane = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        LampCask.wakeRoster()
        val sticky = latch.peekClip()
        if (sticky != null) {
            FogChalk.i("sticky already held")
            waitQuay(started, minWarmupMs)
            return@withContext QuayLane.OpenLamp(
                href = sticky.href,
                restoreHistory = true,
                persistOnPause = true,
            )
        }
        val marked = bag.packBlips()
        val raw = withTimeoutOrNull(14_000) { hopOnce(marked) }
        val fork = decide(raw)
        waitQuay(started, minWarmupMs)
        fork
    }

    private suspend fun hopOnce(marked: JSONObject): String? {
        val result = hop.castRush(marked) ?: return null
        val (status, text) = result
        if (status !in 200..299) return null
        return text
    }

    private fun decide(raw: String?): QuayLane {
        if (raw == null) {
            return latch.peekClip()?.let {
                QuayLane.OpenLamp(it.href, restoreHistory = true, persistOnPause = true)
            } ?: QuayLane.BlankDesk
        }
        return DashCast.parseDash(raw, latch)
    }

    private suspend fun waitQuay(started: Long, minMs: Long) {
        val elapsed = System.currentTimeMillis() - started
        if (elapsed < minMs) delay((minMs - elapsed).milliseconds)
    }

    companion object {
        const val WARMUP_MS = 2250L
    }
}

package com.pixelhaven.beaconrush.gale

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.pixelhaven.beaconrush.MainActivity
import com.pixelhaven.beaconrush.buoy.BuoyClip
import com.pixelhaven.beaconrush.buoy.DashCast
import com.pixelhaven.beaconrush.quay.FogChalk
import com.pixelhaven.beaconrush.quay.GaleWire
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MoleTap : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pushId = intent.getStringExtra(BlipHorn.NOTE_KEY)
        if (!pushId.isNullOrBlank()) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    GaleWire.from(this@MoleTap).reportPulse(pushId, opened = true)
                }.onFailure { FogChalk.w("MoleTap opened report failed", it) }
            }
        }

        val href = intent.getStringExtra(BlipHorn.NAV_HREF)?.takeIf { DashCast.isLiveHref(it) }
        FogChalk.d("MoleTap tap")

        if (MainActivity.relayHref(this, href)) {
            finish()
            return
        }

        when {
            !href.isNullOrBlank() -> {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_LEAP, href)
                        .addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP,
                        ),
                )
            }
            else -> {
                val sticky = BuoyClip(this).peekClip()
                if (sticky != null) {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .putExtra(MainActivity.EXTRA_LEAP, sticky.href)
                            .putExtra(MainActivity.EXTRA_RESTORE, true)
                            .putExtra(MainActivity.EXTRA_CLIP, true)
                            .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
                            ),
                    )
                }
            }
        }
        finish()
    }
}

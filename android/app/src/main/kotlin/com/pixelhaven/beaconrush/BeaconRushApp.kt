package com.pixelhaven.beaconrush

import android.app.Application
import android.webkit.CookieManager
import com.pixelhaven.beaconrush.buoy.BuoyClip
import com.pixelhaven.beaconrush.gale.BlipHorn
import com.pixelhaven.beaconrush.quay.CorkSack
import com.pixelhaven.beaconrush.quay.GaleWire
import com.pixelhaven.beaconrush.quay.PyreJudge

class BeaconRushApp : Application() {
    val corkSack: CorkSack by lazy { CorkSack(this) }
    val galeWire: GaleWire by lazy { GaleWire(this) }
    val buoyClip: BuoyClip by lazy { BuoyClip(this) }
    val pyreJudge: PyreJudge by lazy {
        PyreJudge(
            bag = corkSack,
            hop = galeWire,
            latch = buoyClip,
        )
    }

    override fun onCreate() {
        super.onCreate()
        runCatching {
            CookieManager.getInstance().setAcceptCookie(true)
        }
        BlipHorn.primeBell(this)
        galeWire
    }
}

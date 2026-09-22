package com.pixelhaven.beaconrush.buoy

sealed interface QuayLane {
    data object BlankDesk : QuayLane

    data class OpenLamp(
        val href: String,
        val restoreHistory: Boolean = false,
        val persistOnPause: Boolean = false,
    ) : QuayLane
}

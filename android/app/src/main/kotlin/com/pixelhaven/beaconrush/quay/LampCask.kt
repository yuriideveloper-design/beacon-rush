package com.pixelhaven.beaconrush.quay

internal class LampCask private constructor() {
    companion object {
        init {
            System.loadLibrary("lampnub")
        }

        private val bag: Map<String, String> by lazy { nativeRoster() }

        fun wakeRoster() {
            bag.size
        }

        fun unwind(id: String): String {
            return bag[id] ?: throw IllegalStateException("lamp slot missing")
        }

        @JvmStatic
        external fun nativeRoster(): Map<String, String>
    }
}

package com.pixelhaven.beaconrush.quay

import android.util.Log
import com.pixelhaven.beaconrush.BuildConfig

internal object FogChalk {
    private const val TAG = "FogChalk"

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun w(message: String, error: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (error != null) Log.w(TAG, message, error) else Log.w(TAG, message)
    }

    fun e(message: String, error: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (error != null) Log.e(TAG, message, error) else Log.e(TAG, message)
    }
}

package com.pixelhaven.beaconrush.gale

import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.compose.runtime.staticCompositionLocalOf

interface DockSink {
    fun openFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?,
    ): Boolean

    fun onPermissionRequest(request: PermissionRequest)
}

val LocalDockSink = staticCompositionLocalOf<DockSink> {
    error("DockSink not provided")
}

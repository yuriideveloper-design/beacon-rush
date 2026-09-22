package com.pixelhaven.beaconrush.gale

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CrateNub {
    const val AUTHORITY_SUFFIX = ".0654"

    fun authority(context: Context): String = "${context.packageName}$AUTHORITY_SUFFIX"

    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, authority(context), file)

    fun exportDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    fun captureDir(context: Context): File =
        File(context.cacheDir, "web_capture").apply { mkdirs() }

    fun contentPickIntent(
        acceptTypes: Array<String>,
        multiple: Boolean,
    ): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (acceptTypes.isNotEmpty() && acceptTypes[0].isNotEmpty()) {
                acceptTypes[0]
            } else {
                "*/*"
            }
            if (acceptTypes.size > 1) {
                putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes)
            }
            if (multiple) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
    }

    fun chooserIntent(contentIntent: Intent, extras: List<Intent>, title: String = "Select"): Intent {
        val chooser = Intent.createChooser(contentIntent, title)
        if (extras.isNotEmpty()) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extras.toTypedArray())
        }
        return chooser
    }
}

package com.pixelhaven.beaconrush.gale

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.WebChromeClient
import androidx.core.content.edit
import androidx.core.net.toUri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object TorchBolt {
    private const val TAG = "TorchBolt"
    private const val PREFS_NAME = "beaconrush12.torch.chooser"
    private const val KEY_CAMERA_URI = "camera_uri"

    val PENDING_CAMERA: Array<Uri> = emptyArray()

    fun persistCameraUri(ctx: Context, uri: Uri?) {
        try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit(commit = true) {
                    putString(KEY_CAMERA_URI, uri?.toString())
                }
        } catch (_: Exception) {
        }
    }

    fun loadPersistedCameraUri(ctx: Context): Uri? {
        return try {
            val s = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CAMERA_URI, null)
            if (!s.isNullOrEmpty()) s.toUri() else null
        } catch (_: Exception) {
            null
        }
    }

    fun clearPersistedCameraUri(ctx: Context) {
        persistCameraUri(ctx, null)
    }

    fun buildCameraIntent(activity: Activity, cameraOutHolder: Array<Uri?>): Intent? {
        val output = createCaptureUri(activity) ?: return null
        cameraOutHolder[0] = output
        persistCameraUri(activity, output)

        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, output)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                clipData = ClipData.newUri(activity.contentResolver, "capture", output)
            } catch (_: Exception) {
            }
        }
        grantUriToCameraApps(activity, camera, output)
        return camera
    }

    fun parseResult(
        activity: Activity,
        resultCode: Int,
        data: Intent?,
        cameraOutputUriIn: Uri?,
    ): Array<Uri>? {
        val cameraOutputUri = cameraOutputUriIn ?: loadPersistedCameraUri(activity)
        val camReady = cameraOutputUri != null && uriHasContent(activity, cameraOutputUri)

        if (resultCode != Activity.RESULT_OK) {
            if (!camReady) {
                deleteCaptureFile(activity, cameraOutputUri)
                clearPersistedCameraUri(activity)
                return null
            }
        }

        if (camReady) {
            val out = publishForWebView(activity, cameraOutputUri)
            clearPersistedCameraUri(activity)
            return arrayOf(out ?: cameraOutputUri)
        }

        if (data != null) {
            val parsed = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            if (!parsed.isNullOrEmpty() && parsed[0] != null) {
                deleteCaptureFile(activity, cameraOutputUri)
                clearPersistedCameraUri(activity)
                return parsed
            }
            if (data.data != null) {
                deleteCaptureFile(activity, cameraOutputUri)
                clearPersistedCameraUri(activity)
                return arrayOf(data.data!!)
            }
            val clip = data.clipData
            if (clip != null && clip.itemCount > 0) {
                val uris = Array(clip.itemCount) { clip.getItemAt(it).uri }
                deleteCaptureFile(activity, cameraOutputUri)
                clearPersistedCameraUri(activity)
                return uris
            }
        }

        if (cameraOutputUri != null) {
            return PENDING_CAMERA
        }

        clearPersistedCameraUri(activity)
        return null
    }

    fun publishForWebView(ctx: Context, source: Uri?): Uri? {
        if (source == null) return null
        val compressed = compressCapture(ctx, source)
        val toPublish = compressed ?: source

        try {
            var input = ctx.contentResolver.openInputStream(toPublish)
            if (input == null && toPublish.scheme == "file" && toPublish.path != null) {
                input = FileInputStream(toPublish.path!!)
            }
            if (input == null) return toPublish

            input.use { inStream ->
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "webview_capture_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }
                val dest = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return toPublish
                ctx.contentResolver.openOutputStream(dest)?.use { out ->
                    inStream.copyTo(out)
                    out.flush()
                } ?: return toPublish
                return dest
            }
        } catch (e: Exception) {
            Log.w(TAG, "publishForWebView failed, using source uri", e)
            return toPublish
        }
    }

    fun compressCapture(ctx: Context, source: Uri): Uri? {
        var bitmap: Bitmap? = null
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openUriStream(ctx, source)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null

            val maxSide = 1600
            var sample = 1
            val w = maxOf(1, bounds.outWidth)
            val h = maxOf(1, bounds.outHeight)
            while (maxOf(w, h) / sample > maxSide) sample *= 2

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            bitmap = openUriStream(ctx, source)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null

            val dir = CrateNub.captureDir(ctx)
            val outFile = File(dir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, fos)
                fos.flush()
            }
            return CrateNub.uriForFile(ctx, outFile)
        } catch (e: Exception) {
            Log.w(TAG, "compressCapture failed", e)
            return null
        } finally {
            bitmap?.recycle()
        }
    }

    private fun openUriStream(ctx: Context, uri: Uri) =
        try {
            ctx.contentResolver.openInputStream(uri)
                ?: if (uri.scheme == "file" && uri.path != null) FileInputStream(uri.path!!) else null
        } catch (_: Exception) {
            null
        }

    fun uriHasContent(ctx: Context, uri: Uri): Boolean {
        try {
            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                return pfd.statSize > 0
            }
        } catch (_: Exception) {
        }
        try {
            if (uri.scheme == "file" && uri.path != null) {
                return File(uri.path!!).length() > 0
            }
            if (uri.path?.contains("web_capture") == true) {
                val name = uri.lastPathSegment ?: return false
                val f = File(CrateNub.captureDir(ctx), name)
                return f.exists() && f.length() > 0
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun createCaptureUri(activity: Activity): Uri? {
        return try {
            val dir = CrateNub.captureDir(activity)
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            file.createNewFile()
            try {
                CrateNub.uriForFile(activity, file)
            } catch (_: Exception) {
                createMediaStoreCaptureUri(activity)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createCaptureUri failed", e)
            null
        }
    }

    private fun createMediaStoreCaptureUri(context: Context): Uri? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "webview_capture_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore capture URI failed", e)
            null
        }
    }

    private fun grantUriToCameraApps(activity: Activity, camera: Intent, output: Uri) {
        try {
            val list = activity.packageManager.queryIntentActivities(camera, PackageManager.MATCH_DEFAULT_ONLY)
            for (ri in list) {
                activity.grantUriPermission(
                    ri.activityInfo.packageName,
                    output,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun deleteCaptureFile(ctx: Context, uri: Uri?) {
        if (uri == null) return
        try {
            if (uri.scheme == "file" && uri.path != null) {
                File(uri.path!!).delete()
                return
            }
            if (uri.path?.contains("web_capture") == true) {
                val name = uri.lastPathSegment ?: return
                File(CrateNub.captureDir(ctx), name).delete()
            }
        } catch (_: Exception) {
        }
    }
}

package com.pixelhaven.beaconrush.gale

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pixelhaven.beaconrush.R
import com.pixelhaven.beaconrush.buoy.DashCast
import com.pixelhaven.beaconrush.quay.CorkSack
import com.pixelhaven.beaconrush.quay.FogChalk
import com.pixelhaven.beaconrush.quay.GaleWire
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BlipHorn : FirebaseMessagingService() {

    private val pulseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "beaconrush12_alerts"
        const val NOTE_KEY = "ticket"
        const val NAV_HREF = "go_url"
        private const val CHANNEL_NAME = "Beacon alerts"
        private const val EVENT_PREFS = "beaconrush12.bell.events"
        private const val KEY_SEEN = "seen_ids"

        fun primeBell(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Campaign notifications"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
        }
    }

    override fun onDestroy() {
        pulseScope.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        FogChalk.d("BlipHorn onNewToken len=${token.length}")
        CorkSack(applicationContext).cacheTok(token)
    }

    override fun handleIntent(intent: Intent) {
        val extras = intent.extras
        if (extras != null && extras.containsKey("google.message_id")) {
            FogChalk.d("BlipHorn handleIntent custom tray keys=${extras.keySet()}")
            onMessageReceived(RemoteMessage(extras))
            return
        }
        super.handleIntent(intent)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        FogChalk.d(
            "BlipHorn onMessageReceived from=${message.from} " +
                "notif=${message.notification != null} dataKeys=${message.data.keys}",
        )
        val pushId = message.data[NOTE_KEY]

        if (pushId != null) {
            val dup = isSeen(pushId)
            if (!dup) markSeen(pushId)
            if (dup) return
        }

        val raw = runCatching { message.toIntent()?.extras }.getOrNull()
        val title = message.notification?.title
            ?: message.data["title"]
            ?: raw?.getString("gcm.notification.title")
        val body = message.notification?.body
            ?: message.data["body"]
            ?: raw?.getString("gcm.notification.body")
        if (title.isNullOrBlank() && body.isNullOrBlank()) {
            FogChalk.d("BlipHorn skip empty white payload")
            return
        }

        showNotification(
            title = title?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name),
            body = body.orEmpty(),
            data = message.data,
            pushId = pushId,
            imageUrl = message.notification?.imageUrl?.toString()
                ?: message.data["image"]
                ?: message.data["imageUrl"]
                ?: message.data["image_url"]
                ?: message.data["picture"],
        )
        tellHost(pushId, opened = false)
    }

    private fun messageGoUrl(data: Map<String, String>): String? =
        data[NAV_HREF]?.takeIf { DashCast.isLiveHref(it) }

    private fun tellHost(pushId: String?, opened: Boolean) {
        if (pushId.isNullOrBlank()) return
        pulseScope.launch {
            runCatching {
                GaleWire.from(this@BlipHorn).reportPulse(pushId, opened)
            }.onFailure { FogChalk.w("BlipHorn report failed", it) }
        }
    }

    private fun isSeen(pushId: String): Boolean =
        getSharedPreferences(EVENT_PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_SEEN, emptySet())
            ?.contains(pushId) == true

    private fun markSeen(pushId: String) {
        val prefs = getSharedPreferences(EVENT_PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_SEEN, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(pushId)
        val trimmed = if (set.size > 100) set.toList().takeLast(100).toMutableSet() else set
        prefs.edit().putStringSet(KEY_SEEN, trimmed).apply()
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        pushId: String?,
        imageUrl: String?,
    ) {
        primeBell(this)
        val intent = Intent(this, MoleTap::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            data.forEach { (k, v) -> putExtra(k, v) }
            if (!pushId.isNullOrBlank()) {
                putExtra(NOTE_KEY, pushId)
            }
            messageGoUrl(data)?.let { putExtra(NAV_HREF, it) }
        }
        val notificationId = pushId?.hashCode() ?: System.currentTimeMillis().toInt()
        val pending = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val largeIcon = appIconBitmap()
        val picture = imageUrl?.takeIf { it.startsWith("http") }?.let { fetchBitmap(it) }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_beacon)
            .setLargeIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
            .setColor(ContextCompat.getColor(this, R.color.beacon_push_ink))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (picture != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(picture)
                    .bigLargeIcon(largeIcon)
                    .setSummaryText(body),
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, builder.build())
        FogChalk.d("BlipHorn shown id=$notificationId title=$title")
    }

    private fun appIconBitmap(): Bitmap? {
        val system = runCatching {
            resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        }.getOrDefault(0)
        val size = maxOf(system, 256)
        val raw = runCatching { BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher) }.getOrNull()
        if (raw != null) {
            return rasterizeBitmap(raw, size)
        }
        val drawable = ContextCompat.getDrawable(this, R.mipmap.ic_launcher)
            ?: runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
            ?: return null
        return rasterizeLauncher(drawable, size)
    }

    private fun rasterizeBitmap(source: Bitmap, size: Int): Bitmap {
        if (source.width == size && source.height == size) return source
        val scaled = Bitmap.createScaledBitmap(source, size, size, true)
        return rasterizeLauncher(BitmapDrawable(resources, scaled), size)
    }

    private fun rasterizeLauncher(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val clip = Path().apply {
            addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
        }
        canvas.clipPath(clip)
        val pad = (size * 0.18f).toInt()
        val left = -pad
        val top = -pad
        val right = size + pad
        val bottom = size + pad
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            val layers = LayerDrawable(arrayOf(drawable.background, drawable.foreground))
            layers.setBounds(left, top, right, bottom)
            layers.draw(canvas)
        } else {
            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)
        }
        return bitmap
    }

    private fun fetchBitmap(url: String): Bitmap? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 4_000
        connection.readTimeout = 4_000
        connection.instanceFollowRedirects = true
        connection.connect()
        connection.inputStream.use { BitmapFactory.decodeStream(it) }
    }.onFailure {
        FogChalk.w("BlipHorn image fetch failed", it)
    }.getOrNull()
}

package com.pixelhaven.beaconrush

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.pixelhaven.beaconrush.buoy.QuayLane
import com.pixelhaven.beaconrush.gale.CrateNub
import com.pixelhaven.beaconrush.gale.LocalDockSink
import com.pixelhaven.beaconrush.gale.LoomDeck
import com.pixelhaven.beaconrush.gale.LoomSpec
import com.pixelhaven.beaconrush.gale.DockSink
import com.pixelhaven.beaconrush.gale.SparCage
import com.pixelhaven.beaconrush.gale.TorchBolt
import com.pixelhaven.beaconrush.quay.FogChalk
import com.pixelhaven.beaconrush.quay.QuaySplash
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private sealed interface AppPhase {
    data object Loading : AppPhase
    data object FirstScreen : AppPhase
    data class SecondScreen(
        val url: String,
        val restoreHistory: Boolean,
        val persistOnPause: Boolean,
    ) : AppPhase
}

private fun isCaptureMode(context: Context): Boolean {
    val intent = (context as? Activity)?.intent ?: return false
    return intent.getBooleanExtra("asg_screenshot_mode", false) ||
        intent.getBooleanExtra("asg_screen", false)
}

class MainActivity : FragmentActivity(), DockSink {
    companion object {
        const val EXTRA_FORK = "beacon.fork"
        const val FORK_FIRST = "first"
        const val FORK_SECOND = "second"
        const val EXTRA_LEAP = "beacon.open_href"
        const val EXTRA_PUSH_LEAP = "beacon.push_href"
        const val EXTRA_RESTORE = "beacon.restore"
        const val EXTRA_CLIP = "beacon.keep"

        @Volatile
        var currentInstance: MainActivity? = null
            private set

        @Volatile
        var activeHull: SparCage? = null

        @Volatile
        var webLampShowing: Boolean = false

        fun relayHref(context: Context, url: String?): Boolean {
            val host = currentInstance ?: return false
            if (!webLampShowing) return false
            if (!url.isNullOrBlank()) {
                host.runOnUiThread { activeHull?.loadHref(url) }
            }
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    if (!url.isNullOrBlank()) putExtra(EXTRA_PUSH_LEAP, url)
                },
            )
            return true
        }
    }

    private val _pushDestinations = MutableSharedFlow<PushOpen>(extraBufferCapacity = 1)
    val pushDestinations: SharedFlow<PushOpen> = _pushDestinations.asSharedFlow()

    data class PushOpen(
        val url: String,
        val restoreHistory: Boolean = false,
        val persistOnPause: Boolean = true,
    )

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingFileChooserParams: WebChromeClient.FileChooserParams? = null
    private var cameraOutputUri: Uri? = null
    private var pendingPermissionRequest: PermissionRequest? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            deliverFileChooserResult(result.resultCode, result.data)
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val params = pendingFileChooserParams
            pendingFileChooserParams = null
            if (granted && params != null) {
                openFileChooser(filePathCallback, params, resumeAfterCameraPermission = true)
            } else {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
        }

    private val webPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val req = pendingPermissionRequest
            pendingPermissionRequest = null
            if (req == null) return@registerForActivityResult
            if (grants.values.any { it }) {
                req.grant(req.resources)
            } else {
                req.deny()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentInstance = this
        enableEdgeToEdge()
        enterImmersiveMode()
        setContent {
            CompositionLocalProvider(LocalDockSink provides this@MainActivity) {
                AppEntry()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        absorbHref(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!webLampShowing) enterImmersiveMode()
        tryDeliverPendingCameraCapture()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !webLampShowing) enterImmersiveMode()
    }

    fun leaveImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    fun enterImmersiveMode() {
        if (webLampShowing) return
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onDestroy() {
        if (currentInstance === this) currentInstance = null
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        super.onDestroy()
    }

    fun absorbHref(intent: Intent?) {
        if (intent == null) return
        val url = intent.getStringExtra(EXTRA_PUSH_LEAP)
            ?: intent.getStringExtra(EXTRA_LEAP)
            ?: return
        if (url.isBlank()) return
        FogChalk.d("MainActivity push intent url=$url webLamp=$webLampShowing")
        if (webLampShowing) {
            runOnUiThread { activeHull?.loadHref(url) }
            return
        }
        val restore = intent.getBooleanExtra(EXTRA_RESTORE, false)
        val retain = intent.getBooleanExtra(EXTRA_CLIP, false)
        _pushDestinations.tryEmit(
            PushOpen(
                url = url,
                restoreHistory = restore,
                persistOnPause = retain || restore,
            ),
        )
    }

    override fun openFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?,
    ): Boolean = openFileChooser(filePathCallback, fileChooserParams, resumeAfterCameraPermission = false)

    private fun openFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?,
        resumeAfterCameraPermission: Boolean,
    ): Boolean {
        if (!resumeAfterCameraPermission) {
            if (this.filePathCallback != null) {
                try {
                    this.filePathCallback?.onReceiveValue(null)
                } catch (_: Exception) {
                }
                this.filePathCallback = null
                cameraOutputUri = null
                TorchBolt.clearPersistedCameraUri(this)
            }
            this.filePathCallback = filePathCallback
            val orphan = TorchBolt.loadPersistedCameraUri(this)
            if (orphan != null && TorchBolt.uriHasContent(this, orphan)) {
                cameraOutputUri = orphan
                tryDeliverPendingCameraCapture()
                return true
            }
            cameraOutputUri = null
        }

        return try {
            val acceptTypes = fileChooserParams?.acceptTypes ?: arrayOf()
            val isImageCapture = acceptTypes.any {
                it.startsWith("image/") || it == "image/*" || it.isEmpty()
            }
            val isCaptureEnabled = fileChooserParams?.isCaptureEnabled == true
            val needsCamera = isImageCapture || isCaptureEnabled

            if (needsCamera &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                pendingFileChooserParams = fileChooserParams
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                return true
            }

            val intents = mutableListOf<Intent>()
            if (needsCamera) {
                val holder = arrayOfNulls<Uri>(1)
                val cameraIntent = TorchBolt.buildCameraIntent(this, holder)
                if (cameraIntent != null) {
                    cameraOutputUri = holder[0]
                    intents.add(cameraIntent)
                }
            }

            val contentSelectionIntent = CrateNub.contentPickIntent(
                acceptTypes,
                fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE,
            )
            fileChooserLauncher.launch(CrateNub.chooserIntent(contentSelectionIntent, intents))
            true
        } catch (_: Exception) {
            this.filePathCallback?.onReceiveValue(null)
            this.filePathCallback = null
            true
        }
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        val needed = mutableListOf<String>()
        for (res in request.resources) {
            when (res) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        needed.add(Manifest.permission.CAMERA)
                    }
                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        needed.add(Manifest.permission.RECORD_AUDIO)
                    }
            }
        }
        if (needed.isEmpty()) {
            request.grant(request.resources)
            return
        }
        pendingPermissionRequest = request
        webPermissionLauncher.launch(needed.toTypedArray())
    }

    private fun deliverFileChooserResult(resultCode: Int, data: Intent?) {
        if (filePathCallback == null) {
            cameraOutputUri = null
            TorchBolt.clearPersistedCameraUri(this)
            return
        }
        val results = TorchBolt.parseResult(this, resultCode, data, cameraOutputUri)
        if (results === TorchBolt.PENDING_CAMERA) {
            return
        }
        val callback = filePathCallback
        filePathCallback = null
        cameraOutputUri = null
        try {
            callback?.onReceiveValue(results)
        } catch (_: Exception) {
        }
    }

    private fun tryDeliverPendingCameraCapture() {
        if (filePathCallback == null) return
        val uri = cameraOutputUri ?: TorchBolt.loadPersistedCameraUri(this) ?: return
        if (!TorchBolt.uriHasContent(this, uri)) return
        val published = TorchBolt.publishForWebView(this, uri) ?: uri
        val callback = filePathCallback
        filePathCallback = null
        cameraOutputUri = null
        TorchBolt.clearPersistedCameraUri(this)
        try {
            callback?.onReceiveValue(arrayOf(published))
        } catch (_: Exception) {
        }
    }
}

@Composable
private fun AppEntry() {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val judge = (context.applicationContext as BeaconRushApp).pyreJudge
    val skipGate = remember { isCaptureMode(context) }
    val handedFork = remember {
        val intent = activity?.intent ?: return@remember null
        when (intent.getStringExtra(MainActivity.EXTRA_FORK)) {
            MainActivity.FORK_FIRST -> AppPhase.FirstScreen
            MainActivity.FORK_SECOND -> {
                val url = intent.getStringExtra(MainActivity.EXTRA_LEAP).orEmpty()
                if (url.isBlank()) AppPhase.FirstScreen
                else AppPhase.SecondScreen(
                    url = url,
                    restoreHistory = intent.getBooleanExtra(MainActivity.EXTRA_RESTORE, false),
                    persistOnPause = intent.getBooleanExtra(MainActivity.EXTRA_CLIP, false),
                )
            }
            else -> null
        }
    }
    val coldPush = remember {
        if (handedFork != null) return@remember null
        val intent = activity?.intent ?: return@remember null
        val url = intent.getStringExtra(MainActivity.EXTRA_LEAP)
            ?: intent.getStringExtra(MainActivity.EXTRA_PUSH_LEAP)
        url?.takeIf { it.isNotBlank() }?.let { u ->
            MainActivity.PushOpen(
                url = u,
                restoreHistory = intent.getBooleanExtra(MainActivity.EXTRA_RESTORE, false),
                persistOnPause = intent.getBooleanExtra(MainActivity.EXTRA_CLIP, false) ||
                    intent.getBooleanExtra(MainActivity.EXTRA_RESTORE, false),
            )
        }
    }
    var phase by remember {
        mutableStateOf<AppPhase>(
            when {
                skipGate -> AppPhase.FirstScreen
                handedFork != null -> handedFork
                coldPush != null -> AppPhase.SecondScreen(
                    url = coldPush.url,
                    restoreHistory = coldPush.restoreHistory,
                    persistOnPause = coldPush.persistOnPause,
                )
                else -> AppPhase.Loading
            },
        )
    }
    var gateSuppressed by remember { mutableStateOf(coldPush != null || handedFork != null) }
    var pendingLane by remember { mutableStateOf<QuayLane?>(null) }

    fun applyLane(lane: QuayLane) {
        phase = when (lane) {
            QuayLane.BlankDesk -> AppPhase.FirstScreen
            is QuayLane.OpenLamp -> AppPhase.SecondScreen(
                url = lane.href,
                restoreHistory = lane.restoreHistory,
                persistOnPause = lane.persistOnPause,
            )
        }
    }

    val notificationPermissionResult = remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        FogChalk.d("POST_NOTIFICATIONS granted=$granted")
        notificationPermissionResult.value?.complete(granted)
        notificationPermissionResult.value = null
    }

    LaunchedEffect(activity) {
        val host = activity ?: return@LaunchedEffect
        host.pushDestinations.collect { open ->
            FogChalk.d("AppEntry pushDestinations → SecondScreen url=${open.url}")
            gateSuppressed = true
            phase = AppPhase.SecondScreen(
                url = open.url,
                restoreHistory = open.restoreHistory,
                persistOnPause = open.persistOnPause,
            )
        }
    }

    LaunchedEffect(skipGate, coldPush, handedFork) {
        if (!skipGate && Build.VERSION.SDK_INT >= 33) {
            val already = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (already) {
                FogChalk.d("POST_NOTIFICATIONS already granted")
            } else {
                FogChalk.d("Requesting POST_NOTIFICATIONS…")
                val deferred = CompletableDeferred<Boolean>()
                notificationPermissionResult.value = deferred
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                withTimeoutOrNull(8_000) { deferred.await() }
            }
        } else if (skipGate) {
            FogChalk.d("AppEntry skipGate=true → skip notify prompt")
        } else {
            FogChalk.d("POST_NOTIFICATIONS not required (API < 33)")
        }

        if (skipGate) {
            FogChalk.d("AppEntry skipGate=true → FirstScreen")
            return@LaunchedEffect
        }
        if (handedFork != null) {
            FogChalk.d("AppEntry handed fork")
            return@LaunchedEffect
        }
        if (coldPush != null) {
            FogChalk.d("AppEntry coldPush → SecondScreen url=${coldPush.url} (skip gate)")
            return@LaunchedEffect
        }

        if (gateSuppressed) {
            FogChalk.d("AppEntry gate suppressed by push — skip sortQuay result")
            return@LaunchedEffect
        }

        FogChalk.d("AppEntry sortQuay fallback…")
        val result = runCatching {
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(20_000) { judge.sortQuay() } ?: QuayLane.BlankDesk
            }
        }
            .onFailure { FogChalk.e("sortQuay threw → FirstScreen", it) }
            .getOrDefault(QuayLane.BlankDesk)
        if (gateSuppressed) {
            FogChalk.d("AppEntry gate suppressed after sortQuay — keep push SecondScreen")
            return@LaunchedEffect
        }
        FogChalk.d("AppEntry pendingLane=$result")
        pendingLane = result
        if (result is QuayLane.OpenLamp) {
            applyLane(result)
        }
    }

    when (val current = phase) {
        AppPhase.Loading -> {
            val lane = pendingLane
            QuaySplash(
                readyToLeave = lane != null,
                onFinished = { if (lane != null) applyLane(lane) },
            )
        }
        AppPhase.FirstScreen -> FlutterFirstScreen()
        is AppPhase.SecondScreen -> {
            Box(Modifier.fillMaxSize()) {
                LoomDeck(
                    href = current.url,
                    options = LoomSpec(
                        restoreHistory = current.restoreHistory,
                        persistOnPause = current.persistOnPause,
                    ),
                )
            }
        }
    }
}

@Composable
private fun FlutterFirstScreen() {
    val activity = LocalActivity.current as? FragmentActivity ?: return
    var flutterReady by remember { mutableStateOf(false) }
    DisposableEffect(activity) {
        onDispose {
            activity.supportFragmentManager.findFragmentById(R.id.flare_host)?.let { fragment ->
                activity.supportFragmentManager.commit { remove(fragment) }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = R.id.flare_host
                    setBackgroundColor(0xFF1C1C1C.toInt())
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                    post {
                        val existing = activity.supportFragmentManager
                            .findFragmentById(id) as? FlareHost
                        if (existing != null) {
                            existing.onDisplayed = { flutterReady = true }
                        } else {
                            val host = FlareHost.spawn()
                            host.onDisplayed = { flutterReady = true }
                            activity.supportFragmentManager.commit {
                                replace(id, host)
                            }
                        }
                    }
                }
            },
        )
        if (!flutterReady) {
            QuaySplash()
        }
    }
}

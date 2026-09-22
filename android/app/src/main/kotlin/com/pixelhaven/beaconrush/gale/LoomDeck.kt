package com.pixelhaven.beaconrush.gale

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixelhaven.beaconrush.MainActivity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoomDeck(
    modifier: Modifier = Modifier,
    href: String = "",
    options: LoomSpec = LoomSpec(),
) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val chromeHost = LocalDockSink.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var progress by remember { mutableIntStateOf(0) }
    val hull = remember(chromeHost) {
        SparCage(
            activity = activity,
            chrome = chromeHost,
            onProgress = { progress = it },
        )
    }

    DisposableEffect(hull) {
        MainActivity.activeHull = hull
        MainActivity.webLampShowing = true
        (activity as? MainActivity)?.leaveImmersiveMode()
        onDispose {
            if (MainActivity.activeHull === hull) {
                MainActivity.activeHull = null
                MainActivity.webLampShowing = false
            }
            (activity as? MainActivity)?.enterImmersiveMode()
        }
    }

    DisposableEffect(Unit) {
        val previous = activity.window.attributes.softInputMode
        @Suppress("DEPRECATION")
        activity.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN,
        )
        val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val wasLightStatusBars = insetsController.isAppearanceLightStatusBars
        val wasLightNavBars = insetsController.isAppearanceLightNavigationBars
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
        onDispose {
            activity.window.setSoftInputMode(previous)
            insetsController.isAppearanceLightStatusBars = wasLightStatusBars
            insetsController.isAppearanceLightNavigationBars = wasLightNavBars
        }
    }

    DisposableEffect(hull, href, options) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                hull.keepHold()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            hull.foldHull(save = true)
        }
    }

    BackHandler(enabled = true) {
        if (hull.canRewind()) {
            hull.rewindOnce()
        }
    }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val navBottomPx = WindowInsets.navigationBarsIgnoringVisibility.getBottom(density)
    val reservedNavPx = reservedNavFloor(activity, navBottomPx, density)
    val reservedNavLeftPx = reservedNavSide(
        WindowInsets.navigationBarsIgnoringVisibility.getLeft(density, layoutDirection),
        density,
    )
    val reservedNavRightPx = reservedNavSide(
        WindowInsets.navigationBarsIgnoringVisibility.getRight(density, layoutDirection),
        density,
    )
    val topSafeInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout)

    Box(modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                FrameLayout(viewContext).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    hitchDeckPad(this, activity)
                    hull.mountHull(this)
                    if (href.isNotBlank()) {
                        hull.feedHref(href, options)
                    }
                }
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(topSafeInsets)
                .align(Alignment.TopCenter)
                .background(Color.Black),
        )
        if (reservedNavPx > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { reservedNavPx.toDp() })
                    .align(Alignment.BottomCenter)
                    .background(Color.Black),
            )
        }
        if (reservedNavLeftPx > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { reservedNavLeftPx.toDp() })
                    .align(Alignment.CenterStart)
                    .background(Color.Black),
            )
        }
        if (reservedNavRightPx > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { reservedNavRightPx.toDp() })
                    .align(Alignment.CenterEnd)
                    .background(Color.Black),
            )
        }
        if (progress in 0 until 100) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(topSafeInsets),
                color = Color(0xFFE8A14A),
                trackColor = Color(0xFF1C1C1C).copy(alpha = 0.45f),
            )
        }
    }
}

private fun reservedNavFloor(context: Context, navBottomPx: Int, density: Density): Int {
    val modeId = context.resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
    if (modeId != 0 && context.resources.getInteger(modeId) == 2) {
        return 0
    }
    return reservedNavSide(navBottomPx, density)
}

private fun reservedNavSide(navPx: Int, density: Density): Int {
    val gestureLike = navPx <= with(density) { 32.dp.roundToPx() }
    return if (gestureLike) 0 else navPx
}

private fun hitchDeckPad(host: FrameLayout, activity: ComponentActivity) {
    var imeAnimating = false
    fun applyRoot() {
        val insets = ViewCompat.getRootWindowInsets(host)
            ?: ViewCompat.getRootWindowInsets(activity.window.decorView)
            ?: return
        parkDeckInsets(host, insets)
    }
    ViewCompat.setOnApplyWindowInsetsListener(host) { v, insets ->
        if (!imeAnimating) {
            parkDeckInsets(v, insets)
        } else {
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.translationY = 0f
            v.updatePadding(top = bars.top)
        }
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.setWindowInsetsAnimationCallback(
        host,
        object : WindowInsetsAnimationCompat.Callback(
            WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
        ) {
            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                    imeAnimating = true
                }
            }

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>,
            ): WindowInsetsCompat = insets

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                if (animation.typeMask and WindowInsetsCompat.Type.ime() == 0) return
                imeAnimating = false
                applyRoot()
            }
        },
    )
    applyRoot()
    host.addOnAttachStateChangeListener(
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                ViewCompat.requestApplyInsets(v)
                v.post {
                    ViewCompat.requestApplyInsets(v)
                    applyRoot()
                }
                v.post {
                    v.viewTreeObserver.addOnGlobalLayoutListener(
                        object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                v.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                applyRoot()
                            }
                        },
                    )
                }
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        },
    )
    ViewCompat.requestApplyInsets(host)
}

private fun parkDeckInsets(host: View, insets: WindowInsetsCompat) {
    val bars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
    val status = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())
    val nav = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
    val tappable = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.tappableElement())
    val cutout = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.displayCutout())
    val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
    val imeBottom = if (imeVisible) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
    val bottom = maxOf(bars.bottom, nav.bottom, tappable.bottom, imeBottom, cutout.bottom)
    val landscape = host.resources.configuration.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val top = maxOf(bars.top, cutout.top, status.top)
    val left = if (landscape) maxOf(nav.left, tappable.left) else 0
    val right = if (landscape) maxOf(nav.right, tappable.right) else 0
    host.translationY = 0f
    if (host.paddingTop != top ||
        host.paddingBottom != bottom ||
        host.paddingLeft != left ||
        host.paddingRight != right
    ) {
        host.updatePadding(left = left, top = top, right = right, bottom = bottom)
    }
    if (imeVisible) {
        showDeckCaret(host)
    }
}

private fun showDeckCaret(host: View) {
    val group = host as? ViewGroup ?: return
    val web = (0 until group.childCount)
        .map { group.getChildAt(it) }
        .filterIsInstance<WebView>()
        .firstOrNull()
        ?: return
    web.post {
        web.evaluateJavascript(
            """
            (function(){
              var el = document.activeElement;
              if (!el) return;
              var tag = (el.tagName || '').toLowerCase();
              if (tag !== 'input' && tag !== 'textarea' && !el.isContentEditable) return;
              el.scrollIntoView({block:'nearest', inline:'nearest'});
            })();
            """.trimIndent(),
            null,
        )
    }
}

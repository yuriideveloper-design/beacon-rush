package com.pixelhaven.beaconrush.gale

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Message
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.net.toUri

class JettyLid(
    private val isPopup: Boolean,
    private val chrome: DockSink,
    private val isPrimary: (WebView?) -> Boolean,
    private val onProgress: (Int) -> Unit,
    private val overlayHost: () -> FrameLayout?,
    private val primaryWebView: () -> WebView?,
    private val createPopupWebView: () -> WebView,
    private val overlay: DockTray,
    private val cinema: WideBooth,
    private val onLeaveHref: ((Uri) -> Boolean)? = null,
) : WebChromeClient() {

    class WideBooth {
        var customView: View? = null
        var callback: CustomViewCallback? = null

        val isShowing: Boolean get() = customView != null

        fun hide(primary: WebView?) {
            val view = customView ?: return
            (view.parent as? ViewGroup)?.removeView(view)
            customView = null
            callback?.onCustomViewHidden()
            callback = null
            primary?.visibility = View.VISIBLE
        }
    }

    class DockTray(
        private val activity: Activity,
        private val host: () -> FrameLayout?,
    ) {
        private val stack = ArrayDeque<Pair<View, WebView>>()

        val isShowing: Boolean get() = stack.isNotEmpty()
        val topWebView: WebView? get() = stack.lastOrNull()?.second

        fun present(popup: WebView) {
            val container = host() ?: return
            val density = activity.resources.displayMetrics.density
            val chromeH = (40f * density).toInt()
            val icon = (36f * density).toInt()

            val overlay = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(Color.TRANSPARENT)
            }
            val chromeHeader = FrameLayout(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    chromeH,
                )
                setBackgroundColor(0x99000000.toInt())
            }
            val close = ImageButton(activity).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setBackgroundColor(Color.TRANSPARENT)
                contentDescription = "Close"
                layoutParams = FrameLayout.LayoutParams(icon, icon).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    bottomMargin = (4 * density).toInt()
                    rightMargin = (8 * density).toInt()
                }
                setOnClickListener { dismissTop() }
            }
            chromeHeader.addView(close)
            overlay.addView(chromeHeader)
            popup.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
            overlay.addView(popup)
            container.addView(overlay)
            stack.addLast(overlay to popup)
        }

        fun indexOf(window: WebView): Int = stack.indexOfLast { it.second === window }

        fun dismissTop() {
            if (stack.isEmpty()) return
            dismissAt(stack.lastIndex)
        }

        fun dismissAt(index: Int) {
            if (index !in 0 until stack.size) return
            val (layer, wv) = stack.removeAt(index)
            try {
                wv.stopLoading()
            } catch (_: Exception) {
            }
            try {
                wv.destroy()
            } catch (_: Exception) {
            }
            (layer.parent as? ViewGroup)?.removeView(layer)
        }

        fun dismissAll() {
            while (stack.isNotEmpty()) dismissTop()
        }
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        if (!isPopup && isPrimary(view)) {
            onProgress(newProgress.coerceIn(0, 100))
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean = chrome.openFileChooser(filePathCallback, fileChooserParams)

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request != null) chrome.onPermissionRequest(request)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: android.webkit.GeolocationPermissions.Callback?,
    ) {
        callback?.invoke(origin, true, false)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (isPopup) {
            callback?.onCustomViewHidden()
            return
        }
        if (cinema.customView != null) {
            callback?.onCustomViewHidden()
            return
        }
        val container = overlayHost() ?: return
        cinema.customView = view
        cinema.callback = callback
        primaryWebView()?.visibility = View.GONE
        container.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    override fun onHideCustomView() {
        cinema.hide(primaryWebView())
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?,
    ): Boolean {
        if (view == null) return false
        val extra = view.hitTestResult.extra
        if (!extra.isNullOrBlank()) {
            val href = runCatching { extra.toUri() }.getOrNull()
            if (href != null && onLeaveHref?.invoke(href) == true) {
                return false
            }
        }
        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
        val popup = createPopupWebView()
        overlay.present(popup)
        transport.webView = popup
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(window: WebView?) {
        if (window == null) return
        val idx = overlay.indexOf(window)
        if (idx >= 0) overlay.dismissAt(idx)
    }
}

package com.pixelhaven.beaconrush

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.WebView
import io.flutter.embedding.android.FlutterFragment
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import io.flutter.plugins.GeneratedPluginRegistrant

class FlareHost : FlutterFragment() {
    var onDisplayed: (() -> Unit)? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        flutterEngine.platformViewsController.registry.registerViewFactory(
            "generated_app/terms_webview",
            CharterFactory(),
        )
    }

    override fun onFlutterUiDisplayed() {
        super.onFlutterUiDisplayed()
        onDisplayed?.invoke()
    }

    companion object {
        fun spawn(): FlareHost =
            NewEngineFragmentBuilder(FlareHost::class.java).build()
    }
}

private class CharterFactory :
    PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val url = (args as? Map<*, *>)?.get("url") as? String
            ?: "https://example.com/terms-and-conditions"
        return CharterPane(context, url)
    }
}

@SuppressLint("SetJavaScriptEnabled")
private class CharterPane(context: Context, url: String) : PlatformView {
    private val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        loadUrl(url)
    }

    override fun getView(): View = webView

    override fun dispose() {
        webView.destroy()
    }
}

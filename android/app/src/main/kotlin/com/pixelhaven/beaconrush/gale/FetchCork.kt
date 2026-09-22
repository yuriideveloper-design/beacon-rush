package com.pixelhaven.beaconrush.gale

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream

object FetchCork {
    private const val DOWNLOAD_CHANNEL_ID = "beaconrush12_dl"

    val DOWNLOAD_INTERCEPTOR = """
        (function(){
          if(window.__js9f4aHook)return;
          window.__js9f4aHook=true;
          function extName(mime){
            if(!mime)return 'png';
            if(mime.indexOf('jpeg')>=0||mime.indexOf('jpg')>=0)return 'jpg';
            if(mime.indexOf('png')>=0)return 'png';
            if(mime.indexOf('gif')>=0)return 'gif';
            if(mime.indexOf('webp')>=0)return 'webp';
            if(mime.indexOf('pdf')>=0)return 'pdf';
            return 'png';
          }
          function saveData(url,fname){
            try{
              var head=url.substring(0,url.indexOf(','));
              var mime=head.replace('data:','').replace(';base64','');
              var b64=url.substring(url.indexOf(',')+1);
              var name=fname||('qr_'+Date.now()+'.'+extName(mime));
              if(window.Jsbc47&&Jsbc47.putd5){Jsbc47.putd5(b64,name,mime);}
              else if(window.Jsbc47&&Jsbc47.pingf0){Jsbc47.pingf0(url,mime);}
              return true;
            }catch(err){return false;}
          }
          function saveBlob(url,fname){
            fetch(url).then(function(r){return r.blob();}).then(function(blob){
              var reader=new FileReader();
              reader.onloadend=function(){saveData(reader.result,fname||('qr_'+Date.now()+'.'+extName(blob.type)));};
              reader.readAsDataURL(blob);
            }).catch(function(){});
          }
          function handle(url,fname){
            if(!url)return false;
            if(url.indexOf('data:')===0)return saveData(url,fname);
            if(url.indexOf('blob:')===0){saveBlob(url,fname);return true;}
            return false;
          }
          var aClick=HTMLAnchorElement.prototype.click;
          HTMLAnchorElement.prototype.click=function(){
            try{
              var href=this.href||this.getAttribute('href')||'';
              if((this.hasAttribute('download')||href.indexOf('data:')===0||href.indexOf('blob:')===0)&&handle(href,this.download)){return;}
            }catch(err){}
            return aClick.apply(this,arguments);
          };
          var wOpen=window.open;
          window.open=function(u){
            if(u&&(String(u).indexOf('data:')===0||String(u).indexOf('blob:')===0)){
              handle(String(u),null);
              return {closed:false,close:function(){},focus:function(){},document:{write:function(){},close:function(){}}};
            }
            return wOpen.apply(this,arguments);
          };
          document.addEventListener('click',function(e){
            var a=e.target.closest&&e.target.closest('a[download],a[href^="blob:"],a[href^="data:"]');
            if(!a)return;
            var href=a.href||'';
            if(handle(href,a.download)){e.preventDefault();e.stopPropagation();}
          },true);
        })();
    """.trimIndent()

    fun setup(webView: WebView) {
        val context = webView.context
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            handleDownload(webView, context, url, userAgent, contentDisposition, mimeType)
        }
    }

    fun injectDownloadInterceptor(webView: WebView) {
        webView.evaluateJavascript(DOWNLOAD_INTERCEPTOR, null)
    }

    private fun handleDownload(
        webView: WebView,
        context: Context,
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        if (url.isNullOrEmpty()) {
            Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            when {
                url.startsWith("data:") -> saveDataUri(context, url, mimeType)
                url.startsWith("blob:") -> fetchBlobViaJs(webView, url)
                else -> enqueueHttpDownload(context, url, userAgent, contentDisposition, mimeType)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("NewApi")
    private fun saveDataUri(context: Context, url: String, fallbackMime: String?) {
        val parts = url.split(",", limit = 2)
        if (parts.size < 2) {
            Toast.makeText(context, "Invalid data URI", Toast.LENGTH_SHORT).show()
            return
        }

        val header = parts[0]
        val base64Data = parts[1]
        val fileMimeType = header.substringAfter("data:").substringBefore(";")
            .ifEmpty { fallbackMime ?: "application/octet-stream" }

        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        val extension = mimeTypeToExtension(fileMimeType, url)
        val fileName = "download_${System.currentTimeMillis()}.$extension"
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName,
        )

        FileOutputStream(file).use { it.write(decodedBytes) }
        notifyDownloadComplete(context, fileName)
        scanFile(context, file.absolutePath)
        Toast.makeText(context, "Saved: $fileName", Toast.LENGTH_SHORT).show()
    }

    private fun fetchBlobViaJs(webView: WebView, blobUrl: String) {
        val escapedUrl = blobUrl.replace("'", "\\'")
        val jsCode = """
            (function() {
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '$escapedUrl', true);
                xhr.responseType = 'blob';
                xhr.onload = function() {
                    var reader = new FileReader();
                    reader.onloadend = function() {
                        Jsbc47.pingf0(reader.result, xhr.response.type || '');
                    };
                    reader.readAsDataURL(xhr.response);
                };
                xhr.onerror = function() {
                    Jsbc47.pingf0('', '');
                };
                xhr.send();
            })();
        """.trimIndent()
        webView.evaluateJavascript(jsCode, null)
    }

    private fun enqueueHttpDownload(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        val uri = url.toUri()
        val guessedFileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val fileName = determineFileName(guessedFileName, contentDisposition, mimeType, url)

        val request = DownloadManager.Request(uri).apply {
            setMimeType(mimeType ?: "application/octet-stream")
            addRequestHeader("User-Agent", userAgent ?: "Mozilla/5.0")
            CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotEmpty() }?.let {
                addRequestHeader("Cookie", it)
            }
            setTitle(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            Toast.makeText(context, "Download service unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val downloadId = downloadManager.enqueue(request)
        registerDownloadReceiver(context, downloadManager, downloadId, fileName)
    }

    private fun determineFileName(
        guessedFileName: String?,
        contentDisposition: String?,
        mimeType: String?,
        url: String,
    ): String {
        if (guessedFileName != null && guessedFileName.contains(".") && !guessedFileName.endsWith(".bin")) {
            return guessedFileName
        }
        val dispositionFileName = contentDisposition?.let {
            Regex("filename=\"?([^\"]+)\"?").find(it)?.groupValues?.get(1)
        }
        if (dispositionFileName != null && dispositionFileName.contains(".")) {
            return dispositionFileName
        }
        val mimeExtension = mimeTypeToExtension(mimeType ?: "application/octet-stream", url)
        return "download_${System.currentTimeMillis()}.$mimeExtension"
    }

    private fun mimeTypeToExtension(mimeType: String, url: String = ""): String {
        return when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/jpeg", "image/jpg" -> "jpg"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            "text/plain" -> "txt"
            "text/html" -> "html"
            "application/zip" -> "zip"
            "application/octet-stream", "application/binary" -> guessExtensionFromUrl(url) ?: "bin"
            else -> guessExtensionFromUrl(url) ?: "bin"
        }
    }

    private fun guessExtensionFromUrl(url: String): String? {
        val lastSegment = url.substringAfterLast("/").substringBefore("?").substringBefore("#")
        val ext = lastSegment.substringAfterLast(".", "").lowercase()
        return if (ext.isNotEmpty() && ext.length <= 5 && ext.all { it.isLetterOrDigit() }) ext else null
    }

    private fun notifyDownloadComplete(context: Context, fileName: String) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannelCompat.Builder(
                DOWNLOAD_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT,
            )
                .setName("Downloads")
                .build()
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Download complete")
            .setContentText(fileName)
            .setAutoCancel(true)
            .build()

        try {
            nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (_: SecurityException) {
        }
    }

    private fun scanFile(context: Context, filePath: String) {
        MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerDownloadReceiver(
        context: Context,
        downloadManager: DownloadManager,
        downloadId: Long,
        fileName: String,
    ) {
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("Range")
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
                if (id != downloadId) return

                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIdx >= 0 &&
                            cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL
                        ) {
                            val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            if (uriIdx >= 0) {
                                val localUri = cursor.getString(uriIdx)
                                val filePath = Uri.parse(localUri).path
                                if (!filePath.isNullOrEmpty() && ctx != null) {
                                    notifyDownloadComplete(ctx, fileName)
                                    scanFile(ctx, filePath)
                                }
                            }
                        }
                    }
                }
                try {
                    ctx?.unregisterReceiver(this)
                } catch (_: Exception) {
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    fun saveFromJs(context: Context, dataUrl: String, mime: String?) {
        if (dataUrl.startsWith("data:")) {
            saveDataUri(context, dataUrl, mime)
        }
    }
}

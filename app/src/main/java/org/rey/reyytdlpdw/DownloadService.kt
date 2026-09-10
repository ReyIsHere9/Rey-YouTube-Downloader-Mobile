package org.rey.reyytdlpdw

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import org.json.JSONObject
import java.io.File

/** Runs downloads in the background with a progress notification. */
class DownloadService : Service() {

    companion object {
        const val EXTRA_URLS = "urls"
        const val EXTRA_MODE = "mode"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_AUDIO = "audio"
        const val EXTRA_SUBS = "subs"
        const val EXTRA_LANG = "lang"
        const val CHANNEL = "rey_downloads"
        const val NOTIF_ID = 4211

        fun start(context: Context, urls: ArrayList<String>, mode: String,
                  quality: String, audio: String, subs: Boolean, lang: String) {
            val i = Intent(context, DownloadService::class.java).apply {
                putStringArrayListExtra(EXTRA_URLS, urls)
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_QUALITY, quality)
                putExtra(EXTRA_AUDIO, audio)
                putExtra(EXTRA_SUBS, subs)
                putExtra(EXTRA_LANG, lang)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }
    }

    private val nm by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val urls = intent.getStringArrayListExtra(EXTRA_URLS) ?: arrayListOf()
        val mode = intent.getStringExtra(EXTRA_MODE) ?: "mp4"
        val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "Best"
        val audio = intent.getStringExtra(EXTRA_AUDIO) ?: "mp3"
        val subs = intent.getBooleanExtra(EXTRA_SUBS, false)
        val lang = intent.getStringExtra(EXTRA_LANG) ?: "en"

        createChannel()
        startForeground(NOTIF_ID, buildNotification(0, "Starting\u2026", true))

        Thread {
            runDownloads(urls, mode, quality, audio, subs, lang)
            DownloadState.update(false, 100, "Finished.", "")
            notify(buildNotification(100, "Finished", false))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }.start()
        return START_NOT_STICKY
    }

    private fun runDownloads(urls: List<String>, mode: String, quality: String,
                             audio: String, subs: Boolean, lang: String) {
        val module = Python.getInstance().getModule("download")
        val ffmpeg = File(applicationInfo.nativeLibraryDir, "libffmpeg.so")
            .takeIf { it.exists() }?.path ?: ""
        for ((i, url) in urls.withIndex()) {
            val step = "Downloading ${i + 1}/${urls.size}"
            DownloadState.update(true, 0, step, "")
            notify(buildNotification(0, step, true))
            val cb = PyProgress { pct, text ->
                DownloadState.update(true, pct, step, text)
                notify(buildNotification(pct, text.ifEmpty { step }, true))
            }
            val res: PyObject = try {
                module.callAttr("download", url, mode, quality, audio, subs, lang, ffmpeg, cb)
            } catch (e: Exception) {
                DownloadState.update(true, 0, "Error: ${e.message}", "")
                continue
            }
            try {
                val o = JSONObject(res.toString())
                if (o.optBoolean("ok")) {
                    val title = o.optString("title")
                    val files = o.optJSONArray("files")
                    if (files != null) {
                        for (j in 0 until files.length()) {
                            val fp = File(files.getString(j))
                            if (fp.exists()) {
                                val (uri, path) = FileSaver.save(this, fp)
                                if (uri != null) {
                                    HistoryStore.add(this, HistoryEntry(
                                        title = title.ifEmpty { fp.name },
                                        url = url, mode = mode,
                                        uri = uri.toString(), path = path,
                                        time = System.currentTimeMillis()
                                    ))
                                }
                            }
                        }
                    }
                    DownloadState.update(true, 100, "Saved", title)
                } else {
                    DownloadState.update(true, 0, "Error: " + o.optString("error"), "")
                }
            } catch (e: Exception) {
                DownloadState.update(true, 0, "Error: ${e.message}", "")
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Download progress"
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(pct: Int, text: String, ongoing: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        b.setContentTitle("Rey YouTube Downloader")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
        if (ongoing) b.setProgress(100, pct, pct <= 0)
        return b.build()
    }

    private fun notify(n: Notification) {
        try {
            nm.notify(NOTIF_ID, n)
        } catch (_: Exception) {
        }
    }
}

package org.rey.reyytdlpdw

import android.os.Handler
import android.os.Looper

/** Shared download state so the UI can follow progress across screens. */
object DownloadState {
    @Volatile var running = false
    @Volatile var pct = 0
    @Volatile var status = ""
    @Volatile var current = ""

    private val main = Handler(Looper.getMainLooper())
    @Volatile var listener: (() -> Unit)? = null

    fun update(running: Boolean, pct: Int, status: String, current: String) {
        this.running = running
        this.pct = pct
        this.status = status
        this.current = current
        main.post { listener?.invoke() }
    }
}

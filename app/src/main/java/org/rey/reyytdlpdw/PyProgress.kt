package org.rey.reyytdlpdw

/** Bridge passed to Python; yt-dlp's progress hook calls onProgress(...). */
class PyProgress(private val onUpdate: (Int, String) -> Unit) {
    @Suppress("unused")
    fun onProgress(pct: Int, text: String) {
        onUpdate(pct, text)
    }
}

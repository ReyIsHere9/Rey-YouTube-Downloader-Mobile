package org.rey.reyytdlpdw

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import android.widget.LinearLayout

class HelpActivity : Activity() {

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private val GUIDE = """
How to use
• Paste one or more links (one per line), then press "Preview titles" to see exactly what each link points to.
• If a link is a playlist / channel / multi-video page, every video is listed — untick the ones you don't want, then press Download.

Format
• MP4 — video + audio in one file (the usual download).
• Video only — just the picture, no sound.
• Audio only — sound only, converted to the format you choose.
• Options that don't apply are greyed out automatically.

Quality
• Limits the video resolution. "Best" takes the highest available (can be large). 1080p is a good balance.

Audio format (for Audio only)
• mp3 — most compatible (recommended for music)
• m4a — slightly better quality at the same size
• opus — best sound, fewer devices support it
• wav — lossless, very large (for editing)
• original — keep YouTube's audio untouched

Subtitles
• Pick a language from the list (or "Custom" to type codes like en,en-orig or all).
• In MP4 mode you can embed the subtitles into the file; otherwise they're saved as separate .srt files.

Extras
• Embed thumbnail + metadata puts the cover art and tags inside the file.

Saving & background
• Files go to Downloads/ReyYouTubeDownloader by default — change the folder in Settings.
• Downloads keep running in the background; watch the progress in the notification.

History
• Every download is kept in History so you can Open, Share or Re-download it later.

Install note
• If Google Play Protect warns when installing, that's normal for apps not from the Play Store — tap "Install anyway".
""".trimIndent()

    override fun onCreate(b: Bundle?) {
        setTheme(if (Prefs.isDark(this)) R.style.Theme_Rey else R.style.Theme_Rey_Light)
        super.onCreate(b)
        val t = AppTheme.of(this)

        val scroll = ScrollView(this).apply { setBackgroundColor(t.bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(t.bg)
        }
        root.addView(TextView(this).apply {
            text = "Quick guide"; setTextColor(t.text); textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = GUIDE; setTextColor(t.text); textSize = 14f
            setPadding(0, dp(12), 0, dp(12))
            setLineSpacing(0f, 1.15f)
        })
        scroll.addView(root)
        setContentView(scroll)
    }
}

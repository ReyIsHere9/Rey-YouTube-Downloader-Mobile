package org.rey.reyytdlpdw

import android.app.Activity
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : Activity() {

    private lateinit var theme: AppTheme.C
    private lateinit var list: LinearLayout
    private val df = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        setTheme(if (Prefs.isDark(this)) R.style.Theme_Rey else R.style.Theme_Rey_Light)
        super.onCreate(b)
        theme = AppTheme.of(this)

        val scroll = ScrollView(this).apply { setBackgroundColor(theme.bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(theme.bg)
        }
        root.addView(TextView(this).apply {
            text = "Download history"; setTextColor(theme.text); textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        root.addView(Button(this).apply {
            text = "Clear history"; setBackgroundColor(theme.card); setTextColor(theme.text)
            setOnClickListener {
                HistoryStore.clear(this@HistoryActivity)
                render()
            }
        })
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        list.removeAllViews()
        val items = HistoryStore.list(this)
        if (items.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Nothing yet."; setTextColor(theme.muted); textSize = 14f
                setPadding(0, dp(8), 0, dp(8))
            })
            return
        }
        for (e in items) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(theme.panel)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, dp(6), 0, 0) }
            }
            card.addView(TextView(this).apply {
                text = e.title; setTextColor(theme.text); textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                text = e.mode.uppercase() + "  \u00b7  " + df.format(Date(e.time)) +
                        "\n" + e.path
                setTextColor(theme.muted); textSize = 11f
            })
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val uri = try { Uri.parse(e.uri) } catch (_: Exception) { null }
            actions.addView(small("Open") {
                if (uri != null) ShareUtils.open(this, uri, FileSaver.mimeOf(e.path))
            })
            actions.addView(small("Share") {
                if (uri != null) ShareUtils.share(this, uri, FileSaver.mimeOf(e.path), e.title)
            })
            actions.addView(small("Re-download") {
                DownloadService.start(this, arrayListOf(e.url), e.mode, "Best", "mp3",
                    false, "en", false, true)
            })
            actions.addView(small("Remove") {
                HistoryStore.remove(this, e)
                if (uri != null) {
                    try { contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                }
                render()
            })
            card.addView(actions)
            list.addView(card)
        }
    }

    private fun small(text: String, on: () -> Unit) = Button(this).apply {
        this.text = text
        setBackgroundColor(theme.card); setTextColor(theme.text); textSize = 12f
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        setOnClickListener { on() }
    }
}

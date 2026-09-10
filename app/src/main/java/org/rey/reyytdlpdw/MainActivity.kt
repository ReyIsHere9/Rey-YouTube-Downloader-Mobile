package org.rey.reyytdlpdw

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var theme: AppTheme.C
    private lateinit var url: EditText
    private lateinit var log: TextView
    private lateinit var listWrap: LinearLayout
    private lateinit var bar: ProgressBar
    private lateinit var status: TextView
    private lateinit var formatSp: Spinner
    private lateinit var qualitySp: Spinner
    private lateinit var audioSp: Spinner
    private lateinit var subCb: CheckBox
    private lateinit var subLang: EditText
    private var appliedDark = false
    private val rowUrls = mutableListOf<String>()
    private val rowCbs = mutableListOf<CheckBox>()

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun py() = Python.getInstance()

    private fun vspace(h: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
    }

    private fun section(s: String) = TextView(this).apply {
        text = s.uppercase()
        setTextColor(theme.muted)
        textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun rowLabel(s: String) = TextView(this).apply {
        text = s; setTextColor(theme.text); textSize = 14f
    }

    private fun spinner(values: Array<String>) = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity,
            android.R.layout.simple_spinner_dropdown_item, values)
        setBackgroundColor(theme.card)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun styledButton(text: String, bg: Int, fg: Int): Button = Button(this).apply {
        this.text = text
        setBackgroundColor(bg)
        setTextColor(fg)
    }

    override fun onCreate(b: Bundle?) {
        appliedDark = Prefs.isDark(this)
        setTheme(if (appliedDark) R.style.Theme_Rey else R.style.Theme_Rey_Light)
        super.onCreate(b)
        theme = AppTheme.of(this)

        val outer = ScrollView(this)
        outer.setBackgroundColor(theme.bg)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(theme.bg)
        }

        // header
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        header.addView(TextView(this).apply {
            text = "Rey YouTube Downloader"
            setTextColor(theme.text); textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val hist = TextView(this).apply {
            text = "History"; setTextColor(theme.accent); textSize = 14f
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { startActivity(Intent(this@MainActivity, HistoryActivity::class.java)) }
        }
        val gear = TextView(this).apply {
            text = "\u2699"; setTextColor(theme.text); textSize = 20f
            setPadding(dp(8), 0, 0, 0)
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        header.addView(hist); header.addView(gear)
        root.addView(header)
        root.addView(vspace(dp(8)))

        // url
        url = EditText(this).apply {
            hint = "Paste one or more YouTube links, one per line"
            setTextColor(theme.text); setHintTextColor(theme.muted)
            setBackgroundColor(theme.panel)
            minLines = 3; gravity = Gravity.TOP
        }
        root.addView(url)
        val urlRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        urlRow.addView(styledButton("Paste", theme.card, theme.text).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val txt = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                if (txt.isNotEmpty()) url.setText(txt)
            }
        })
        urlRow.addView(styledButton("Clear", theme.card, theme.text).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { url.setText(""); listWrap.removeAllViews() }
        })
        root.addView(urlRow)
        root.addView(vspace(dp(6)))

        root.addView(section("Format"))
        formatSp = spinner(arrayOf("MP4 (video + audio)", "Video only", "Audio only"))
        root.addView(formatSp)

        root.addView(section("Quality"))
        qualitySp = spinner(arrayOf("Best", "2160p", "1440p", "1080p", "720p", "480p", "360p"))
        qualitySp.setSelection(3)
        root.addView(qualitySp)

        root.addView(section("Audio format (Audio only)"))
        audioSp = spinner(arrayOf("mp3", "m4a", "opus", "wav", "original"))
        root.addView(audioSp)

        root.addView(section("Subtitles"))
        val subRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        subCb = CheckBox(this).apply { text = "Download"; setTextColor(theme.text) }
        subRow.addView(subCb)
        subLang = EditText(this).apply {
            setText("en"); hint = "lang"
            setTextColor(theme.text); setHintTextColor(theme.muted)
            setBackgroundColor(theme.panel)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        subRow.addView(subLang)
        root.addView(subRow)

        root.addView(vspace(dp(6)))
        val prev = styledButton("Preview titles", theme.card, theme.text)
        prev.setOnClickListener { preview() }
        root.addView(prev)

        val listScroll = ScrollView(this)
        listWrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listScroll.addView(listWrap)
        listScroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(240))
        root.addView(listScroll)

        val dl = styledButton("Download", theme.accent, theme.onAccent)
        dl.textSize = 16f
        dl.setOnClickListener { startDownload() }
        root.addView(dl)

        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progress = 0
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        root.addView(bar)
        status = TextView(this).apply {
            text = "Ready."; setTextColor(theme.muted); textSize = 12f
        }
        root.addView(status)

        log = TextView(this).apply {
            setTextColor(theme.muted); text = ""; textSize = 11f
        }
        val logScroll = ScrollView(this)
        logScroll.addView(log)
        logScroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(180))
        root.addView(logScroll)

        outer.addView(root)
        setContentView(outer)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onStart() {
        super.onStart()
        DownloadState.listener = { refreshProgress() }
        refreshProgress()
    }

    override fun onStop() {
        super.onStop()
        DownloadState.listener = null
    }

    override fun onResume() {
        super.onResume()
        if (Prefs.isDark(this) != appliedDark) recreate()
    }

    private fun refreshProgress() {
        bar.progress = DownloadState.pct
        val t = when {
            DownloadState.running -> DownloadState.status +
                    (if (DownloadState.current.isNotEmpty()) "  \u00b7  ${DownloadState.current}" else "")
            DownloadState.status.isNotEmpty() -> DownloadState.status
            else -> "Ready."
        }
        status.text = t
        if (DownloadState.status.isNotEmpty() && !DownloadState.running) {
            appendLog(DownloadState.status)
        }
    }

    private fun appendLog(t: String) {
        log.text = (log.text.toString() + "\n" + t).trim()
    }

    private fun links(): List<String> =
        url.text.toString().split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    private fun preview() {
        if (links().isEmpty()) { appendLog("Paste a link first."); return }
        appendLog("Looking up\u2026")
        val mod = py().getModule("download")
        thread {
            try {
                val res: PyObject = mod.callAttr("preview", url.text.toString())
                val arr = res.asList()
                runOnUiThread {
                    listWrap.removeAllViews()
                    rowUrls.clear(); rowCbs.clear()
                    var found = 0
                    for (o in arr) {
                        val pair = (o as PyObject).asList()
                        if (pair.size < 2) continue
                        val title = pair[0]?.toString() ?: ""
                        val u = pair[1]?.toString() ?: ""
                        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                        val cb = CheckBox(this).apply { isChecked = true }
                        row.addView(cb)
                        row.addView(TextView(this).apply {
                            text = title; setTextColor(theme.text); textSize = 13f
                            layoutParams = LinearLayout.LayoutParams(
                                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        })
                        rowUrls.add(u); rowCbs.add(cb)
                        listWrap.addView(row)
                        found++
                    }
                    appendLog(if (found == 0) "No videos found." else "Found $found video(s).")
                }
            } catch (e: Exception) {
                runOnUiThread { appendLog("Preview error: ${e.message}") }
            }
        }
    }

    private fun startDownload() {
        if (DownloadState.running) { appendLog("Already downloading."); return }
        val targets = mutableListOf<String>()
        for (i in rowUrls.indices) if (i < rowCbs.size && rowCbs[i].isChecked) targets.add(rowUrls[i])
        if (targets.isEmpty()) targets.addAll(links())
        if (targets.isEmpty()) { appendLog("Paste a link first."); return }
        val mode = when (formatSp.selectedItemPosition) {
            1 -> "video"; 2 -> "audio"; else -> "mp4"
        }
        val quality = if (qualitySp.selectedItemPosition == 0) "Best"
        else qualitySp.selectedItem.toString()
        val audio = audioSp.selectedItem.toString()
        val lang = subLang.text.toString().trim().ifEmpty { "en" }
        appendLog("Queued ${targets.size} download(s)\u2026")
        DownloadService.start(this, ArrayList(targets), mode, quality, audio,
            subCb.isChecked, lang)
    }
}

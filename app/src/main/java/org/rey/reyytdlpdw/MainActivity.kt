package org.rey.reyytdlpdw

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var url: EditText
    private lateinit var log: TextView
    private lateinit var listWrap: LinearLayout

    // options
    private lateinit var formatSp: Spinner
    private lateinit var qualitySp: Spinner
    private lateinit var audioSp: Spinner
    private lateinit var subCb: CheckBox
    private lateinit var subLang: EditText
    private lateinit var autoCb: CheckBox

    private val rowUrls = mutableListOf<String>()
    private val rowCbs = mutableListOf<CheckBox>()

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()
    private fun py() = Python.getInstance()

    private fun vspace(h: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
    }

    private fun section(s: String) = TextView(this).apply {
        text = s.uppercase()
        setTextColor(Color.parseColor("#9E9E9E"))
        textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun rowLabel(s: String) = TextView(this).apply {
        text = s
        setTextColor(Color.WHITE)
        textSize = 14f
    }

    private fun spinner(values: Array<String>) = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity,
            android.R.layout.simple_spinner_dropdown_item, values)
        setBackgroundColor(Color.parseColor("#1E1E1E"))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))

        val outer = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(16), dp(16), dp(16))
        root.setBackgroundColor(Color.parseColor("#0F0F0F"))

        val header = TextView(this).apply {
            text = "Rey YouTube Downloader"
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(header)
        root.addView(vspace(dp(8)))

        url = EditText(this).apply {
            hint = "Paste one or more YouTube links, one per line"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            minLines = 3
            gravity = Gravity.TOP
        }
        root.addView(url)
        root.addView(vspace(dp(8)))

        root.addView(section("Format"))
        formatSp = spinner(arrayOf("MP4 (video + audio)", "Video only", "Audio only"))
        root.addView(formatSp)
        root.addView(vspace(dp(6)))

        root.addView(section("Quality"))
        qualitySp = spinner(arrayOf("Best", "2160p", "1440p", "1080p", "720p", "480p", "360p"))
        qualitySp.setSelection(3)
        root.addView(qualitySp)
        root.addView(vspace(dp(6)))

        root.addView(section("Audio format (Audio only)"))
        audioSp = spinner(arrayOf("mp3", "m4a", "opus", "wav", "original"))
        root.addView(audioSp)
        root.addView(vspace(dp(6)))

        root.addView(section("Subtitles"))
        val subRow = LinearLayout(this)
        subRow.orientation = LinearLayout.HORIZONTAL
        subCb = CheckBox(this).apply { text = "Download" }
        subRow.addView(subCb)
        subLang = EditText(this).apply {
            setText("en")
            hint = "lang"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        subRow.addView(subLang)
        autoCb = CheckBox(this).apply { text = "Auto" }
        autoCb.isChecked = true
        subRow.addView(autoCb)
        root.addView(subRow)
        root.addView(vspace(dp(8)))

        val prev = android.widget.Button(this).apply { text = "Preview titles" }
        prev.setOnClickListener { preview() }
        root.addView(prev)

        val listScroll = ScrollView(this)
        listWrap = LinearLayout(this)
        listWrap.orientation = LinearLayout.VERTICAL
        listScroll.addView(listWrap)
        listScroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(260))
        root.addView(listScroll)

        val dl = android.widget.Button(this).apply {
            text = "Download"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#E11D2A"))
        }
        dl.setOnClickListener { downloadSelected() }
        root.addView(dl)

        log = TextView(this).apply {
            setTextColor(Color.parseColor("#C9C9C9"))
            text = ""
            textSize = 12f
        }
        val logScroll = ScrollView(this)
        logScroll.addView(log)
        logScroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(220))
        root.addView(logScroll)

        outer.addView(root)
        setContentView(outer)
    }

    private fun appendLog(t: String) {
        log.text = log.text.toString() + "\n" + t
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
                        val title = pair.get(0)?.toString() ?: ""
                        val u = pair.get(1)?.toString() ?: ""
                        val row = LinearLayout(this)
                        row.orientation = LinearLayout.HORIZONTAL
                        val cb = CheckBox(this)
                        cb.isChecked = true
                        row.addView(cb)
                        val tv = TextView(this).apply {
                            text = title
                            setTextColor(Color.parseColor("#DDDDDD"))
                            textSize = 13f
                            layoutParams = LinearLayout.LayoutParams(
                                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        row.addView(tv)
                        rowUrls.add(u); rowCbs.add(cb)
                        listWrap.addView(row)
                        found++
                    }
                    if (found == 0) appendLog("No videos found.")
                    else appendLog("Found " + found + " video(s).")
                }
            } catch (e: Exception) {
                runOnUiThread { appendLog("Preview error: " + e.message) }
            }
        }
    }

    private fun opts(): Qual {
        val m = when (formatSp.selectedItemPosition) {
            1 -> "video"
            2 -> "audio"
            else -> "mp4"
        }
        return Qual(
            mode = m,
            quality = if (qualitySp.selectedItemPosition == 0) "Best"
                      else qualitySp.selectedItem.toString(),
            audio = audioSp.selectedItem.toString(),
            subs = subCb.isChecked,
            subLang = subLang.text.toString().trim().ifEmpty { "en" }
        )
    }

    private data class Qual(
        val mode: String, val quality: String, val audio: String,
        val subs: Boolean, val subLang: String
    )

    private fun downloadSelected() {
        val q = opts()
        val targets = mutableListOf<String>()
        for (i in rowUrls.indices) {
            if (i < rowCbs.size && rowCbs[i].isChecked) {
                targets.add(rowUrls[i])
            }
        }
        if (targets.isEmpty()) {
            val ls = links()
            if (ls.isEmpty()) { appendLog("Paste a link first."); }
            else { for (u in ls) targets.add(u) }
        }
        if (targets.isEmpty()) { appendLog("Nothing to download."); return }
        appendLog("Downloading\u2026")
        val mod = py().getModule("download")
        thread {
            try {
                for (u in targets) {
                    val r: PyObject = mod.callAttr(
                        "download", u, q.mode, q.quality, q.audio, q.subs, q.subLang)
                    runOnUiThread { appendLog(r.toString()) }
                }
                runOnUiThread { appendLog("Finished.") }
            } catch (e: Exception) {
                runOnUiThread { appendLog("Download error: " + e.message) }
            }
        }
    }
}

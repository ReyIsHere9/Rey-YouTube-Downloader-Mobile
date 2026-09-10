package org.rey.reyytdlpdw

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import java.net.HttpURLConnection
import java.net.URL
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
    private lateinit var langSp: Spinner
    private lateinit var customLang: EditText
    private lateinit var embedSubsCb: CheckBox
    private lateinit var embedThumbCb: CheckBox
    private lateinit var qualityRow: View
    private lateinit var audioRow: View
    private lateinit var langRow: View
    private lateinit var embedSubsRow: View
    private var appliedDark = false
    private val rowUrls = mutableListOf<String>()
    private val rowCbs = mutableListOf<CheckBox>()
    private val thumbCache = HashMap<String, Bitmap>()
    private val thumbLoading = HashSet<String>()
    private val main = Handler(Looper.getMainLooper())

    private val LANGS = listOf(
        "English" to "en", "Spanish" to "es", "French" to "fr", "German" to "de",
        "Portuguese" to "pt", "Italian" to "it", "Japanese" to "ja", "Korean" to "ko",
        "Hindi" to "hi", "Arabic" to "ar", "Russian" to "ru", "Chinese" to "zh",
        "All languages" to "all", "Custom\u2026" to ""
    )

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun py() = Python.getInstance()

    private fun rounded(color: Int, radiusDp: Int = 12) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radiusDp).toFloat()
    }

    private fun vspace(h: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
    }

    private fun section(s: String) = TextView(this).apply {
        text = s.uppercase(); setTextColor(theme.muted); textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(4), dp(8), 0, dp(2))
    }

    private fun label(s: String) = TextView(this).apply {
        text = s; setTextColor(theme.text); textSize = 14f
    }

    private fun spinner(values: List<String>) = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity,
            android.R.layout.simple_spinner_dropdown_item, values)
        background = rounded(theme.card, 10)
        setPadding(dp(8), dp(6), dp(8), dp(6))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun button(text: String, bg: Int, fg: Int, radius: Int = 12): Button =
        Button(this).apply {
            this.text = text; setTextColor(fg); textSize = 14f
            background = rounded(bg, radius)
            stateListAnimator = null
        }

    private fun card(vararg children: View): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(theme.panel, 14)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(6), 0, 0) }
        }
        for (ch in children) c.addView(ch)
        return c
    }

    private fun headerIcon(text: String, on: () -> Unit) = TextView(this).apply {
        this.text = text; setTextColor(theme.text); textSize = 20f
        setPadding(dp(10), 0, 0, 0)
        setOnClickListener { on() }
    }

    override fun onCreate(b: Bundle?) {
        appliedDark = Prefs.isDark(this)
        setTheme(if (appliedDark) R.style.Theme_Rey else R.style.Theme_Rey_Light)
        super.onCreate(b)
        theme = AppTheme.of(this)

        val outer = ScrollView(this).apply { setBackgroundColor(theme.bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
            setBackgroundColor(theme.bg)
        }

        // ---- header ----
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ImageView(this).apply {
            setImageResource(R.drawable.logo)
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        })
        header.addView(TextView(this).apply {
            text = "Rey Downloader"; setTextColor(theme.text); textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            ellipsize = TextUtils.TruncateAt.END; maxLines = 1
        })
        header.addView(headerIcon("\u2753") { startActivity(Intent(this, HelpActivity::class.java)) })
        header.addView(headerIcon("\u2630") { startActivity(Intent(this, HistoryActivity::class.java)) })
        header.addView(headerIcon("\u2699") { startActivity(Intent(this, SettingsActivity::class.java)) })
        root.addView(header)
        root.addView(vspace(dp(8)))

        // ---- url card ----
        url = EditText(this).apply {
            hint = "Paste one or more YouTube links, one per line"
            setTextColor(theme.text); setHintTextColor(theme.muted)
            background = rounded(theme.card, 10)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            minLines = 3; gravity = Gravity.TOP
        }
        val urlRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        urlRow.addView(button("Paste", theme.card, theme.text, 10).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val t = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                if (t.isNotEmpty()) url.setText(t)
            }
        })
        urlRow.addView(button("Clear", theme.card, theme.text, 10).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { url.setText(""); listWrap.removeAllViews() }
        })
        root.addView(card(url, urlRow))

        // ---- format / quality / audio ----
        formatSp = spinner(listOf("MP4 (video + audio)", "Video only", "Audio only"))
        qualitySp = spinner(listOf("Best", "2160p", "1440p", "1080p", "720p", "480p", "360p"))
        qualitySp.setSelection(3)
        audioSp = spinner(listOf("mp3", "m4a", "opus", "wav", "original"))

        qualityRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label("Video quality")); addView(qualitySp)
        }
        audioRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label("Audio format")); addView(audioSp)
        }
        root.addView(card(
            section("Format"), formatSp,
            section("Quality"), qualityRow,
            section("Audio"), audioRow
        ))
        formatSp.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) = applyEnable()
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        // ---- subtitles ----
        subCb = CheckBox(this).apply { text = "Download subtitles"; setTextColor(theme.text) }
        langSp = spinner(LANGS.map { it.first })
        customLang = EditText(this).apply {
            hint = "custom code, e.g. en,en-orig"; setTextColor(theme.text)
            setHintTextColor(theme.muted); background = rounded(theme.card, 10)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        langRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label("Language")); addView(langSp); addView(customLang)
        }
        embedSubsCb = CheckBox(this).apply {
            text = "Embed subtitles into MP4"; setTextColor(theme.text); isChecked = true
        }
        embedSubsRow = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(embedSubsCb) }
        subCb.setOnCheckedChangeListener { _, _ -> applyEnable() }
        langSp.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) = applyEnable()
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
        root.addView(card(section("Subtitles"), subCb, langRow, embedSubsRow))

        // ---- embed ----
        embedThumbCb = CheckBox(this).apply {
            text = "Embed thumbnail + metadata"; setTextColor(theme.text); isChecked = true
        }
        root.addView(card(section("Extras"), embedThumbCb))

        // ---- preview + download ----
        val prev = button("Preview titles", theme.card, theme.text)
        prev.setOnClickListener { preview() }
        root.addView(card(prev))

        val listScroll = ScrollView(this)
        listWrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listScroll.addView(listWrap)
        listScroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(240))
        root.addView(listScroll)

        val dl = button("Download", theme.accent, theme.onAccent, 14).apply { textSize = 17f }
        dl.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, dp(10), 0, 0) }
        dl.setOnClickListener { startDownload() }
        root.addView(dl)

        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progress = 0
        }
        root.addView(bar)
        status = TextView(this).apply { text = "Ready."; setTextColor(theme.muted); textSize = 12f }
        root.addView(status)

        log = TextView(this).apply { setTextColor(theme.muted); text = ""; textSize = 11f }
        val logScroll = ScrollView(this)
        logScroll.addView(log)
        logScroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(140))
        root.addView(logScroll)

        outer.addView(root)
        setContentView(outer)
        applyEnable()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun setEnabled(view: View, on: Boolean) {
        view.isEnabled = on
        view.alpha = if (on) 1f else 0.35f
    }

    private fun applyEnable() {
        val fmt = formatSp.selectedItemPosition // 0 mp4, 1 video, 2 audio
        val video = fmt != 2

        qualitySp.isEnabled = video
        qualityRow.alpha = if (video) 1f else 0.35f

        audioSp.isEnabled = !video
        audioRow.alpha = if (!video) 1f else 0.35f

        val subs = subCb.isChecked
        val custom = langSp.selectedItemPosition == LANGS.size - 1
        langSp.isEnabled = subs
        customLang.isEnabled = subs && custom
        customLang.visibility = if (subs && custom) View.VISIBLE else View.GONE
        langRow.alpha = if (subs) 1f else 0.35f

        val canEmbed = subs && fmt == 0
        embedSubsCb.isEnabled = canEmbed
        embedSubsRow.alpha = if (canEmbed) 1f else 0.35f
    }

    private fun selectedLang(): String {
        if (langSp.selectedItemPosition == LANGS.size - 1) {
            return customLang.text.toString().trim().ifEmpty { "en" }
        }
        return LANGS[langSp.selectedItemPosition].second
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
                        val thumb = if (pair.size > 2) pair[2]?.toString() ?: "" else ""
                        listWrap.addView(videoRow(title, u, thumb))
                        found++
                    }
                    appendLog(if (found == 0) "No videos found." else "Found $found video(s).")
                }
            } catch (e: Exception) {
                runOnUiThread { appendLog("Preview error: ${e.message}") }
            }
        }
    }

    private fun videoRow(title: String, u: String, thumb: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = rounded(theme.panel, 12)
            setPadding(dp(8), dp(6), dp(8), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(4), 0, 0) }
        }
        val iv = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(80), dp(45))
            setBackgroundColor(theme.card)
        }
        row.addView(iv)
        if (thumb.isNotEmpty()) loadThumb(thumb, iv)
        val cb = CheckBox(this).apply { isChecked = true }
        row.addView(cb)
        row.addView(TextView(this).apply {
            text = title; setTextColor(theme.text); textSize = 13f
            setPadding(dp(6), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 2; ellipsize = TextUtils.TruncateAt.END
        })
        rowUrls.add(u); rowCbs.add(cb)
        return row
    }

    private fun loadThumb(urlStr: String, iv: ImageView) {
        thumbCache[urlStr]?.let { iv.setImageBitmap(it); return }
        if (thumbLoading.contains(urlStr)) return
        thumbLoading.add(urlStr)
        thread {
            try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000; conn.readTimeout = 10000
                val bmp = BitmapFactory.decodeStream(conn.inputStream)
                if (bmp != null) {
                    thumbCache[urlStr] = bmp
                    main.post { iv.setImageBitmap(bmp) }
                }
            } catch (_: Exception) {
            } finally {
                thumbLoading.remove(urlStr)
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
        val lang = selectedLang()
        appendLog("Queued ${targets.size} download(s)\u2026")
        DownloadService.start(
            this, ArrayList(targets), mode, quality, audio,
            subCb.isChecked, lang,
            subCb.isChecked && embedSubsCb.isChecked && mode == "mp4",
            embedThumbCb.isChecked
        )
    }
}

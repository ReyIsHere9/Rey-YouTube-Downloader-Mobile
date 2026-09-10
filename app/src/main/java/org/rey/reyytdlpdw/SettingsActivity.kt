package org.rey.reyytdlpdw

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var theme: AppTheme.C
    private lateinit var saveLabel: TextView
    private var appliedDark = false
    private val REQ_TREE = 41

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun vspace(h: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
    }

    override fun onCreate(b: Bundle?) {
        appliedDark = Prefs.isDark(this)
        setTheme(if (appliedDark) R.style.Theme_Rey else R.style.Theme_Rey_Light)
        super.onCreate(b)
        theme = AppTheme.of(this)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(theme.bg)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(theme.bg)
        }

        root.addView(TextView(this).apply {
            text = "Settings"; setTextColor(theme.text); textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(vspace(dp(12)))

        // ---- Theme ----
        root.addView(label("Appearance"))
        val rg = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val rbSystem = RadioButton(this).apply { text = "System default"; setTextColor(theme.text) }
        val rbLight = RadioButton(this).apply { text = "Light"; setTextColor(theme.text) }
        val rbDark = RadioButton(this).apply { text = "Dark"; setTextColor(theme.text) }
        rg.addView(rbSystem); rg.addView(rbLight); rg.addView(rbDark)
        when (Prefs.theme(this)) {
            Prefs.THEME_LIGHT -> rbLight.isChecked = true
            Prefs.THEME_DARK -> rbDark.isChecked = true
            else -> rbSystem.isChecked = true
        }
        rg.setOnCheckedChangeListener { _, id ->
            val v = when (id) {
                rbLight.id -> Prefs.THEME_LIGHT
                rbDark.id -> Prefs.THEME_DARK
                else -> Prefs.THEME_SYSTEM
            }
            Prefs.setTheme(this, v)
            recreate()
        }
        root.addView(rg)
        root.addView(vspace(dp(12)))

        // ---- Save location ----
        root.addView(label("Save location"))
        val rl = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val rbDefault = RadioButton(this).apply {
            text = "Downloads/ReyYouTubeDownloader (default)"; setTextColor(theme.text)
        }
        val rbCustom = RadioButton(this).apply { text = "Custom folder"; setTextColor(theme.text) }
        rl.addView(rbDefault); rl.addView(rbCustom)
        if (Prefs.saveMode(this) == Prefs.SAVE_CUSTOM) rbCustom.isChecked = true
        else rbDefault.isChecked = true
        rbDefault.setOnClickListener { Prefs.setSaveMode(this, Prefs.SAVE_MEDIASTORE); updateSaveLabel() }
        rbCustom.setOnClickListener { pickFolder() }
        root.addView(rl)

        saveLabel = TextView(this).apply {
            setTextColor(theme.muted); textSize = 12f
            setPadding(0, dp(4), 0, 0)
        }
        root.addView(saveLabel)
        val choose = Button(this).apply {
            text = "Choose folder\u2026"; setBackgroundColor(theme.card); setTextColor(theme.text)
        }
        choose.setOnClickListener { pickFolder() }
        root.addView(choose)
        root.addView(vspace(dp(12)))

        // ---- Notifications ----
        root.addView(label("Notifications"))
        val sw = Switch(this).apply {
            text = "Show download progress notification"
            setTextColor(theme.text)
            isChecked = Prefs.notif(this@SettingsActivity)
            setOnCheckedChangeListener { _, v -> Prefs.setNotif(this@SettingsActivity, v) }
        }
        root.addView(sw)
        root.addView(vspace(dp(20)))

        root.addView(Button(this).apply {
            text = "Done"; setBackgroundColor(theme.accent); setTextColor(theme.onAccent)
            setOnClickListener { finish() }
        })

        scroll.addView(root)
        setContentView(scroll)
        updateSaveLabel()
    }

    private fun label(s: String) = TextView(this).apply {
        text = s.uppercase(); setTextColor(theme.muted); textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun updateSaveLabel() {
        saveLabel.text = if (Prefs.saveMode(this) == Prefs.SAVE_CUSTOM)
            "Custom: " + (Prefs.treeUri(this) ?: "not set")
        else "Public Downloads folder"
    }

    private fun pickFolder() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(i, REQ_TREE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_TREE && resultCode == RESULT_OK && data?.data != null) {
            val uri: Uri = data.data!!
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } catch (_: Exception) {
            }
            Prefs.setTreeUri(this, uri.toString())
            Prefs.setSaveMode(this, Prefs.SAVE_CUSTOM)
            updateSaveLabel()
        }
    }
}

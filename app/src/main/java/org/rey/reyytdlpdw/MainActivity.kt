package org.rey.reyytdlpdw

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var url: EditText
    private lateinit var log: TextView
    private lateinit var listWrap: LinearLayout

    private fun vspace(h: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))

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
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        root.addView(url)
        root.addView(vspace(dp(8)))

        val prev = Button(this).apply { text = "Preview titles" }
        prev.setOnClickListener { preview() }
        root.addView(prev)

        val listScroll = ScrollView(this)
        listWrap = LinearLayout(this)
        listWrap.orientation = LinearLayout.VERTICAL
        listScroll.addView(listWrap)
        listScroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        )
        root.addView(listScroll)

        val dl = Button(this).apply {
            text = "Download"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#E11D2A"))
        }
        dl.setOnClickListener { downloadAll() }
        root.addView(dl)

        log = TextView(this).apply {
            setTextColor(Color.parseColor("#C9C9C9"))
            text = ""
            textSize = 12f
        }
        val logScroll = ScrollView(this)
        logScroll.addView(log)
        logScroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.5f
        )
        root.addView(logScroll)

        setContentView(root)
    }

    private fun dp(n: Int): Int =
        (n * resources.displayMetrics.density).toInt()

    private fun py() = Python.getInstance()
    private fun links(): List<String> =
        url.text.toString().split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    private fun appendLog(t: String) {
        log.text = log.text.toString() + "\n" + t
    }

    private fun preview() {
        val ls = links()
        if (ls.isEmpty()) { appendLog("Paste a link first."); return }
        appendLog("Looking up\u2026")
        val mod = py().getModule("download")
        thread {
            val res: PyObject = mod.callAttr("preview", ls)
            val arr = res.asList()
            runOnUiThread {
                listWrap.removeAllViews()
                for (o in arr) {
                    listWrap.addView(TextView(this).apply {
                        text = "• " + o.toString()
                        setTextColor(Color.parseColor("#DDDDDD"))
                    })
                }
                appendLog("Found " + arr.size + " video(s).")
            }
        }
    }

    private fun downloadAll() {
        val ls = links()
        if (ls.isEmpty()) { appendLog("Paste a link first."); return }
        appendLog("Downloading\u2026")
        val mod = py().getModule("download")
        thread {
            for (u in ls) {
                val r: PyObject = mod.callAttr("download", u)
                runOnUiThread { appendLog(r.toString()) }
            }
            runOnUiThread { appendLog("Finished.") }
        }
    }
}

package org.rey.reyytdlpdw

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val title: String,
    val url: String,
    val mode: String,
    val uri: String,
    val path: String,
    val time: Long
)

object HistoryStore {
    private const val FILE = "rey_prefs"
    private const val KEY = "history"
    private const val CAP = 300

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun list(c: Context): MutableList<HistoryEntry> {
        val out = mutableListOf<HistoryEntry>()
        val raw = sp(c).getString(KEY, null) ?: return out
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    HistoryEntry(
                        o.optString("title"), o.optString("url"), o.optString("mode"),
                        o.optString("uri"), o.optString("path"), o.optLong("time")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun save(c: Context, items: List<HistoryEntry>) {
        val arr = JSONArray()
        for (e in items) {
            val o = JSONObject()
            o.put("title", e.title); o.put("url", e.url); o.put("mode", e.mode)
            o.put("uri", e.uri); o.put("path", e.path); o.put("time", e.time)
            arr.put(o)
        }
        sp(c).edit().putString(KEY, arr.toString()).apply()
    }

    fun add(c: Context, e: HistoryEntry) {
        val items = list(c)
        items.add(0, e)
        while (items.size > CAP) items.removeAt(items.size - 1)
        save(c, items)
    }

    fun remove(c: Context, e: HistoryEntry) {
        val items = list(c)
        items.removeAll { it.time == e.time && it.uri == e.uri && it.title == e.title }
        save(c, items)
    }

    fun clear(c: Context) = sp(c).edit().remove(KEY).apply()
}

package org.rey.reyytdlpdw

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val FILE = "rey_prefs"
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val SAVE_MEDIASTORE = "mediastore"
    const val SAVE_CUSTOM = "custom"

    private fun sp(c: Context): SharedPreferences =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun theme(c: Context): String = sp(c).getString("theme", THEME_SYSTEM) ?: THEME_SYSTEM
    fun setTheme(c: Context, v: String) = sp(c).edit().putString("theme", v).apply()

    fun saveMode(c: Context): String = sp(c).getString("saveMode", SAVE_MEDIASTORE) ?: SAVE_MEDIASTORE
    fun setSaveMode(c: Context, v: String) = sp(c).edit().putString("saveMode", v).apply()

    fun treeUri(c: Context): String? = sp(c).getString("treeUri", null)
    fun setTreeUri(c: Context, v: String?) = sp(c).edit().putString("treeUri", v).apply()

    fun notif(c: Context): Boolean = sp(c).getBoolean("notif", true)
    fun setNotif(c: Context, v: Boolean) = sp(c).edit().putBoolean("notif", v).apply()

    fun isDark(c: Context): Boolean {
        return when (theme(c)) {
            THEME_LIGHT -> false
            THEME_DARK -> true
            else -> (c.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }
}

package org.rey.reyytdlpdw

import android.content.Context

object AppTheme {
    data class C(
        val bg: Int, val panel: Int, val card: Int, val text: Int,
        val muted: Int, val accent: Int, val border: Int, val onAccent: Int
    )

    fun dark() = C(
        bg = 0xFF0F0F0F.toInt(), panel = 0xFF141414.toInt(), card = 0xFF1E1E1E.toInt(),
        text = 0xFFF1F1F1.toInt(), muted = 0xFF9E9E9E.toInt(),
        accent = 0xFFE11D2A.toInt(), border = 0xFF2B2B2B.toInt(),
        onAccent = 0xFFFFFFFF.toInt()
    )

    fun light() = C(
        bg = 0xFFFFFFFF.toInt(), panel = 0xFFF3F3F3.toInt(), card = 0xFFEAEAEA.toInt(),
        text = 0xFF141414.toInt(), muted = 0xFF666666.toInt(),
        accent = 0xFFD31225.toInt(), border = 0xFFCFCFCF.toInt(),
        onAccent = 0xFFFFFFFF.toInt()
    )

    fun of(ctx: Context): C = if (Prefs.isDark(ctx)) dark() else light()
}

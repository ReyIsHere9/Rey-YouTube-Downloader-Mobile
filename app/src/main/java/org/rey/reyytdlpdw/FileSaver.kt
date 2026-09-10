package org.rey.reyytdlpdw

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File

/** Copies a finished download into the user's chosen location. */
object FileSaver {

    fun mimeOf(name: String): String {
        val e = name.substringAfterLast('.', "").lowercase()
        return when (e) {
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/opus"
            "ogg", "oga" -> "audio/ogg"
            "wav" -> "audio/wav"
            "srt", "vtt", "ass" -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }

    /** Returns (uri, displayPath). uri may be null on failure. */
    fun save(context: Context, temp: File): Pair<Uri?, String> {
        val name = temp.name
        val mime = mimeOf(name)

        if (Prefs.saveMode(context) == Prefs.SAVE_CUSTOM) {
            val tree = Prefs.treeUri(context)
            if (tree != null) {
                try {
                    val treeUri = Uri.parse(tree)
                    val docTree = DocumentFile.fromTreeUri(context, treeUri)
                    if (docTree != null && docTree.canWrite()) {
                        docTree.findFile(name)?.delete()
                        val doc = docTree.createFile(mime, name)
                        if (doc != null) {
                            context.contentResolver.openOutputStream(doc.uri)?.use { out ->
                                temp.inputStream().use { it.copyTo(out) }
                            }
                            return doc.uri to name
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }

        // Default: public Downloads via MediaStore
        return if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/ReyYouTubeDownloader")
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = try {
                context.contentResolver.insert(collection, values)
            } catch (_: Exception) {
                null
            }
            if (uri != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        temp.inputStream().use { it.copyTo(out) }
                    }
                    return uri to ("Download/ReyYouTubeDownloader/" + name)
                } catch (_: Exception) {
                }
            }
            null to ""
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "ReyYouTubeDownloader"
            )
            dir.mkdirs()
            val dest = File(dir, name)
            try {
                temp.copyTo(dest, overwrite = true)
                val uri = FileProvider.getUriForFile(
                    context, context.packageName + ".fileprovider", dest)
                return uri to dest.absolutePath
            } catch (_: Exception) {
                null to ""
            }
        }
    }
}

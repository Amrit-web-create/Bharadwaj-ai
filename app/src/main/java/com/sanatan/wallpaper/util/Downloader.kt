package com.sanatan.wallpaper.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.sanatan.wallpaper.data.model.Wallpaper
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object Downloader {
    const val ASSET_PREFIX = "file:///android_asset/"

    private fun openStream(ctx: Context, w: Wallpaper, saved: File?): InputStream {
        if (saved != null && saved.exists() && saved.length() > 0) return saved.inputStream()
        if (w.url.startsWith(ASSET_PREFIX)) return ctx.assets.open(w.url.removePrefix(ASSET_PREFIX))
        val c = URL(w.url).openConnection() as HttpURLConnection
        c.connectTimeout = 12000
        c.readTimeout = 20000
        if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
        return c.inputStream
    }

    /** Copy image into app storage (for the Saved tab / offline use). */
    fun cacheToFile(ctx: Context, w: Wallpaper, target: File) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".part")
        openStream(ctx, w, null).use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
        tmp.renameTo(target)
    }

    /** Save to gallery as e.g. "Hanuman Wallpaper 0.1". Returns the file name (without extension). */
    fun saveToGallery(ctx: Context, w: Wallpaper, saved: File?, name: String): String {
        val lower = w.url.lowercase()
        val (ext, mime) = when {
            lower.contains(".png") -> "png" to "image/png"
            lower.contains(".webp") -> "webp" to "image/webp"
            else -> "jpg" to "image/jpeg"
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.$ext")
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Sanatan Wallpaper")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = ctx.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Gallery access unavailable")
        try {
            openStream(ctx, w, saved).use { i ->
                val out = resolver.openOutputStream(uri) ?: throw IOException("Gallery write error")
                out.use { o -> i.copyTo(o) }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return name
    }
}

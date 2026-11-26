package com.sanatan.wallpaper.data.local

import android.content.Context
import java.io.File

/** Offline copy of wallpapers.json */
class CatalogCache(ctx: Context) {
    private val file = File(ctx.filesDir, "catalog_cache.json")

    fun read(): String? =
        if (file.exists()) runCatching { file.readText() }.getOrNull() else null

    fun write(text: String) {
        runCatching {
            val tmp = File(file.parentFile, "catalog_cache.tmp")
            tmp.writeText(text)
            tmp.renameTo(file)
        }
    }
}

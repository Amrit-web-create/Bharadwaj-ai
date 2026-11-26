package com.sanatan.wallpaper.data.local

import android.content.Context
import com.sanatan.wallpaper.data.model.Wallpaper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Liked wallpapers: metadata json + image file in app storage (works offline). */
class SavedStore(ctx: Context) {
    private val dir = File(ctx.filesDir, "saved").also { it.mkdirs() }
    private val meta = File(ctx.filesDir, "saved_meta.json")

    @Synchronized
    fun load(): List<Wallpaper> {
        if (!meta.exists()) return emptyList()
        return runCatching {
            val a = JSONArray(meta.readText())
            (0 until a.length()).map { idx ->
                val o = a.getJSONObject(idx)
                val t = o.optJSONArray("tags")
                Wallpaper(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    category = o.getString("category"),
                    source = o.optString("source"),
                    tags = if (t == null) emptyList() else (0 until t.length()).map { n -> t.getString(n) },
                    url = o.getString("url"),
                    isLocal = o.optBoolean("local")
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    private fun write(list: List<Wallpaper>) {
        val a = JSONArray()
        list.forEach { w ->
            val o = JSONObject()
            o.put("id", w.id)
            o.put("title", w.title)
            o.put("category", w.category)
            o.put("source", w.source)
            o.put("url", w.url)
            o.put("local", w.isLocal)
            o.put("tags", JSONArray(w.tags))
            a.put(o)
        }
        meta.writeText(a.toString())
    }

    fun add(w: Wallpaper): List<Wallpaper> {
        val n = listOf(w) + load().filter { it.id != w.id }
        write(n)
        return n
    }

    fun remove(w: Wallpaper): List<Wallpaper> {
        val n = load().filter { it.id != w.id }
        write(n)
        runCatching { fileFor(w).delete() }
        return n
    }

    fun fileFor(w: Wallpaper): File {
        val safe = w.id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(60)
        return File(dir, safe + "_" + Integer.toHexString(w.id.hashCode()))
    }

    fun hasFile(w: Wallpaper): Boolean = fileFor(w).let { it.exists() && it.length() > 0 }
}

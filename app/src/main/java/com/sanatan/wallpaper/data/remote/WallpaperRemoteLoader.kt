package com.sanatan.wallpaper.data.remote

import com.sanatan.wallpaper.data.model.Wallpaper
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object WallpaperRemoteLoader {
    const val CATALOG_URL = "https://wallpaper-assets-5l7.pages.dev/wallpapers.json"
    private const val BASE = "https://wallpaper-assets-5l7.pages.dev/"

    fun fetchRaw(): String {
        val c = URL(CATALOG_URL).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10000
            c.readTimeout = 15000
            c.requestMethod = "GET"
            c.setRequestProperty("Cache-Control", "no-cache")
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun parse(text: String): List<Wallpaper> {
        val t = text.trim().removePrefix("\uFEFF")
        val arr: JSONArray = if (t.startsWith("[")) {
            JSONArray(t)
        } else {
            val o = JSONObject(t)
            listOf("wallpapers", "items", "data", "images", "catalog")
                .firstNotNullOfOrNull { o.optJSONArray(it) } ?: JSONArray()
        }
        val out = ArrayList<Wallpaper>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val url = o.optString("url").trim()
            if (url.isEmpty()) continue
            val full = if (url.startsWith("http")) url else BASE + url.removePrefix("/")
            val id = o.opt("id")?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: full
            val title = o.optString("title").ifBlank { id }
            val tg = o.opt("tags")
            val tags: List<String> = when (tg) {
                is JSONArray -> (0 until tg.length()).map { tg.optString(it) }
                is String -> tg.split(",", ";")
                else -> emptyList()
            }.map { it.trim() }.filter { it.isNotBlank() }
            out.add(
                Wallpaper(
                    id = id,
                    title = title.trim(),
                    category = o.optString("category").ifBlank { "General" }.trim(),
                    source = o.optString("source").trim(),
                    tags = tags,
                    url = full
                )
            )
        }
        return out.distinctBy { it.id }
    }
}

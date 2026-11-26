package com.sanatan.wallpaper.data.local

import android.content.Context
import com.sanatan.wallpaper.data.model.Wallpaper
import org.json.JSONArray
import org.json.JSONObject

/** Interest weights, liked ids, search history, download counter. */
class UserPrefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("sanatan_prefs", Context.MODE_PRIVATE)

    private fun readMap(key: String): MutableMap<String, Double> {
        val s = sp.getString(key, null) ?: return mutableMapOf()
        return runCatching {
            val o = JSONObject(s)
            val m = mutableMapOf<String, Double>()
            val it = o.keys()
            while (it.hasNext()) {
                val k = it.next()
                m[k] = o.optDouble(k, 0.0)
            }
            m
        }.getOrDefault(mutableMapOf())
    }

    private fun writeMap(key: String, m: Map<String, Double>) {
        val o = JSONObject()
        for ((k, v) in m) o.put(k, v)
        sp.edit().putString(key, o.toString()).apply()
    }

    private fun add(key: String, name: String, amount: Double) {
        val k = name.trim().lowercase()
        if (k.isEmpty()) return
        val m = readMap(key)
        if (amount > 0) {
            for (e in m.keys.toList()) m[e] = (m[e] ?: 0.0) * 0.985
        }
        m[k] = maxOf(0.0, (m[k] ?: 0.0) + amount)
        writeMap(key, m)
    }

    fun bumpCategory(category: String, amount: Double) = add("cat", category, amount)

    fun bump(w: Wallpaper, amount: Double) {
        add("cat", w.category, amount)
        w.tags.forEach { add("tag", it, amount) }
    }

    fun catInterest(): Map<String, Double> = readMap("cat")
    fun tagInterest(): Map<String, Double> = readMap("tag")

    private fun readList(key: String): List<String> {
        val s = sp.getString(key, null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(s)
            (0 until a.length()).map { a.getString(it) }
        }.getOrDefault(emptyList())
    }

    private fun writeList(key: String, l: List<String>) {
        sp.edit().putString(key, JSONArray(l).toString()).apply()
    }

    fun likedIds(): Set<String> = readList("liked").toSet()

    fun setLiked(id: String, liked: Boolean) {
        val s = readList("liked").toMutableList()
        s.remove(id)
        if (liked) s.add(id)
        writeList("liked", s)
    }

    fun history(): List<String> = readList("history")

    fun addHistory(q: String): List<String> {
        val t = q.trim()
        if (t.length < 2) return history()
        val l = readList("history").toMutableList()
        l.removeAll { it.equals(t, ignoreCase = true) }
        l.add(0, t)
        val n = l.take(15)
        writeList("history", n)
        return n
    }

    fun removeHistory(q: String): List<String> {
        val n = readList("history").filter { it != q }
        writeList("history", n)
        return n
    }

    fun clearHistory() = writeList("history", emptyList())

    fun downloadCount(category: String): Int =
        readMap("dl_cat")[category.lowercase()]?.toInt() ?: 0

    fun setDownloadCount(category: String, n: Int) {
        val m = readMap("dl_cat")
        m[category.lowercase()] = n.toDouble()
        writeMap("dl_cat", m)
    }

    /** 0 = follow system, 1 = light (default), 2 = dark */
    fun themeMode(): Int = sp.getInt("theme", 1)
    fun setThemeMode(m: Int) = sp.edit().putInt("theme", m).apply()

    /** images per row: 2 or 4 */
    fun gridCols(): Int = sp.getInt("grid", 2)
    fun setGridCols(n: Int) = sp.edit().putInt("grid", n).apply()
}

package com.sanatan.wallpaper.domain

import com.sanatan.wallpaper.data.model.Wallpaper

/** Typo tolerant search over title, category and tags (>= ~62% similarity). */
object SmartSearchEngine {
    private const val THRESHOLD = 0.62
    private val spaces = Regex("\\s+")

    fun normalize(s: String): String =
        s.lowercase().map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("").trim().replace(spaces, " ")

    fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return 1.0 - levenshtein(a, b).toDouble() / maxOf(a.length, b.length)
    }

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    private fun tokenScore(q: String, cand: String): Double {
        if (cand == q) return 1.0
        if (q.length < 3 || cand.length < 3) return 0.0
        if (cand.startsWith(q)) return 0.92
        if (cand.contains(q)) return 0.85
        val sim = similarity(q, cand)
        return if (sim >= THRESHOLD) sim * 0.9 else 0.0
    }

    fun search(query: String, items: List<Wallpaper>): List<Wallpaper> {
        val qFull = normalize(query)
        if (qFull.isEmpty()) return emptyList()
        val qTokens = qFull.split(" ")
        val need = maxOf(1, (qTokens.size + 1) / 2)
        val scored = ArrayList<Pair<Wallpaper, Double>>()

        for (w in items) {
            val fields = ArrayList<Pair<String, Double>>()
            val t0 = normalize(w.title)
            if (!(t0.length > 20 && !t0.contains(' '))) fields.add(t0 to 3.0)
            fields.add(normalize(w.category) to 2.0)
            for (t in w.tags) fields.add(normalize(t) to 2.5)

            var total = 0.0
            var matched = 0
            for (q in qTokens) {
                var best = 0.0
                for ((f, wt) in fields) {
                    if (f.isEmpty()) continue
                    for (t in f.split(" ")) {
                        val s = tokenScore(q, t) * wt
                        if (s > best) best = s
                    }
                }
                if (best > 0) { matched++; total += best }
            }
            var phrase = 0.0
            if (qFull.length >= 3) {
                for ((f, wt) in fields) {
                    if (f.isEmpty()) continue
                    val s = similarity(qFull, f)
                    if (s >= THRESHOLD) phrase = maxOf(phrase, s * wt * qTokens.size)
                }
            }
            if (matched >= need || phrase > 0) scored.add(w to (total + phrase))
        }
        return scored.sortedByDescending { it.second }.map { it.first }
    }
}

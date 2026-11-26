package com.sanatan.wallpaper.domain

import com.sanatan.wallpaper.data.model.Wallpaper
import kotlin.random.Random

/**
 * Mixed explore feed:
 *  - every category is interleaved (round-robin), never one category in a long run
 *  - high-interest categories get more slots per round and their best (interest/tag matched)
 *    wallpapers come first, but all other categories still keep showing up.
 */
object ExploreFeedEngine {
    fun build(
        all: List<Wallpaper>,
        catInterest: Map<String, Double>,
        tagInterest: Map<String, Double>,
        seed: Long
    ): List<Wallpaper> {
        if (all.isEmpty()) return all
        val rnd = Random(seed)

        fun score(w: Wallpaper): Double {
            var s = catInterest[w.category.lowercase()] ?: 0.0
            for (t in w.tags) s += 0.5 * (tagInterest[t.lowercase()] ?: 0.0)
            return s
        }

        val groups: Map<String, ArrayDeque<Wallpaper>> = all.groupBy { it.category.lowercase() }
            .mapValues { (_, list) ->
                ArrayDeque(
                    list.map { it to (score(it) + rnd.nextDouble() * 1.5) }
                        .sortedByDescending { it.second }
                        .map { it.first }
                )
            }
        val interest = groups.keys.associateWith { catInterest[it] ?: 0.0 }
        val maxI = interest.values.maxOrNull() ?: 0.0
        val order = groups.keys.toList().shuffled(rnd).sortedByDescending { interest[it] ?: 0.0 }

        val out = ArrayList<Wallpaper>(all.size)
        while (out.size < all.size) {
            var added = false
            for (k in order) {
                val q = groups[k] ?: continue
                val quota = if (maxI > 0) 1 + (((interest[k] ?: 0.0) / maxI) * 2).toInt() else 1
                repeat(quota) {
                    val next = q.removeFirstOrNull()
                    if (next != null) {
                        out.add(next)
                        added = true
                    }
                }
            }
            if (!added) break
        }
        return out
    }
}

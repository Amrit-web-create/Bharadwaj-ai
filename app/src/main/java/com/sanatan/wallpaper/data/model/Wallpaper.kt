package com.sanatan.wallpaper.data.model

data class Wallpaper(
    val id: String,
    val title: String,
    val category: String,
    val source: String,
    val tags: List<String>,
    val url: String,
    val isLocal: Boolean = false
)

/** "hanuman" -> "Hanuman" */
fun prettyCategory(c: String): String =
    c.trim().lowercase().replaceFirstChar { it.uppercase() }

/** Name used for downloads: "<Category> Wallpaper" */
fun downloadBaseName(w: Wallpaper): String {
    val c = prettyCategory(w.category).replace(Regex("[\\\\/:*?\"<>|]"), "").trim()
    return if (c.isEmpty() || c.equals("Local", true)) "Sanatan Wallpaper" else "$c Wallpaper"
}

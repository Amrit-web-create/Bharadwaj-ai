package com.sanatan.wallpaper.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

object AppInfo {
    /** Link that is sent when a user shares. Works once the app is on Play Store. */
    const val APP_LINK = "https://play.google.com/store/apps/details?id=com.sanatan.wallpaper"
    const val CONTACT_EMAIL = "theunkhownman999@gmail.com"
}

object ShareUtil {
    private fun message(): String =
        "Beautiful Sanatan wallpapers \uD83D\uDE4F\nDownload the app: " + AppInfo.APP_LINK

    private fun sendIntent(): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message())
    }

    /** pkg = specific app (WhatsApp / Telegram); null = system share sheet. Falls back to share sheet. */
    fun share(ctx: Context, pkg: String?) {
        try {
            if (pkg != null) {
                ctx.startActivity(sendIntent().setPackage(pkg))
                return
            }
        } catch (e: Exception) {
            // app not installed -> fall through to chooser
        }
        try {
            ctx.startActivity(Intent.createChooser(sendIntent(), "Share via"))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Unable to share", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyLink(ctx: Context) {
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("Sanatan Wallpaper", AppInfo.APP_LINK))
        Toast.makeText(ctx, "Link copied", Toast.LENGTH_SHORT).show()
    }
}

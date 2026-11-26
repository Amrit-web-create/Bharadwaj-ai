package com.sanatan.wallpaper.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanatan.wallpaper.util.AppInfo
import com.sanatan.wallpaper.util.ShareUtil

private fun safeLaunch(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(ctx, "Unable to open", Toast.LENGTH_SHORT).show()
    }
}

// ---------------- Settings tab ----------------
@Composable
fun SettingsTab(vm: MainViewModel, pad: PaddingValues, onPage: (String) -> Unit) {
    val ctx = LocalContext.current
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(22.dp)

    Column(
        Modifier
            .fillMaxSize()
            .padding(pad)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Settings", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Pal.text,
            modifier = Modifier.padding(top = 12.dp, bottom = 18.dp)
        )

        Text("Appearance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Pal.accentDeep, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Pal.card).padding(4.dp)) {
            for ((m, label) in listOf(1 to "Light", 2 to "Dark", 0 to "System")) {
                val sel = mode == m
                Box(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) Pal.accent else Color.Transparent)
                        .clickable { vm.setTheme(m) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (sel) Pal.onAccent else Pal.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().clip(shape).background(Pal.card)) {
            SettingsRow("Rate Us", { Icon(Icons.Default.Star, null, tint = Pal.accentDeep, modifier = Modifier.size(24.dp)) }) { onPage("rate") }
            Divider1()
            SettingsRow("Privacy Policy", { Icon(Icons.Default.Lock, null, tint = Pal.accentDeep, modifier = Modifier.size(24.dp)) }) { onPage("privacy") }
            Divider1()
            SettingsRow("About Us", { Icon(Icons.Default.Info, null, tint = Pal.accentDeep, modifier = Modifier.size(24.dp)) }) { onPage("about") }
            Divider1()
            SettingsRow("Help", { Text("?", color = Pal.accentDeep, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }) { onPage("help") }
            Divider1()
            SettingsRow("Contact Us", { Icon(Icons.Default.Email, null, tint = Pal.accentDeep, modifier = Modifier.size(24.dp)) }) { onPage("contact") }
        }

        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth().clip(shape).background(Pal.accentSoft).border(1.dp, Pal.accent.copy(alpha = 0.5f), shape).padding(20.dp)
        ) {
            Text("Share App to your Friends", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Pal.text)
            Spacer(Modifier.height(4.dp))
            Text("Loving the app? Let your friends know about it too!", fontSize = 15.sp, lineHeight = 21.sp, color = Pal.muted)
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Share Now", Modifier.width(190.dp)) { ShareUtil.share(ctx, null) }
        }

        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sanatan Wallpaper", fontWeight = FontWeight.Bold, color = Pal.text, fontSize = 16.sp)
            Text("Version 1.0.0", color = Pal.muted, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("All rights reserved", color = Pal.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRow(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Pal.accentSoft),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Pal.text)
        Text("\u203A", fontSize = 28.sp, color = Pal.accentDeep)
    }
}

@Composable
private fun Divider1() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Pal.line))
}

// ---------------- Sub pages ----------------
@Composable
fun SettingsPageOverlay(page: String, onBack: () -> Unit) {
    BackHandler { onBack() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.bg)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        when (page) {
            "about" -> AboutPage(onBack)
            "privacy" -> PrivacyPage(onBack)
            "rate" -> RatePage(onBack)
            "help" -> HelpPage(onBack)
            else -> ContactPage(onBack)
        }
    }
}

@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Pal.accentSoft).border(1.dp, Pal.accent.copy(alpha = 0.5f), CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Pal.accentDeep) }
            Spacer(Modifier.width(14.dp))
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Pal.text)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            content = content
        )
    }
}

@Composable
private fun Para(text: String) {
    Text(
        text,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        color = Pal.text.copy(alpha = 0.88f),
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun AboutPage(onBack: () -> Unit) {
    SubPage("About Us", onBack) {
        Para("Sanatan Wallpaper brings you beautiful, high quality devotional wallpapers of Mahadev, Shri Ram, Krishna, Hanuman, Ganesh, Durga and many more.")
        Para("Your home feed mixes every category and slowly learns what you love, so the wallpapers you like appear first. Search by the name of a deity or a tag - even if you make a spelling mistake.")
        Para("Like a wallpaper to keep it in Liked and open it anytime, even without internet. Download in one tap and it is saved to your gallery.")
        Para("Made with devotion for everyone who loves Sanatan Dharma.")
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PrivacyPage(onBack: () -> Unit) {
    SubPage("Privacy Policy", onBack) {
        Para("Your privacy matters to us. Sanatan Wallpaper is built to work with as little personal information as possible.")
        Para("What we store: your liked wallpapers, search history, the categories you like and your theme choice, so we can personalise your feed. All of this stays on your own device. We do not send it to any server.")
        Para("What we do not do: we do not ask for your name, phone number, email or location, there is no account or login, and we never sell or share your data with anyone.")
        Para("Internet use: the app only connects to the internet to load the wallpaper list and images.")
        Para("Downloads: when you tap Download, the image is saved in the Pictures folder of your phone.")
        Para("Your control: remove any search from Recent searches, or remove a wallpaper from Liked by tapping its heart. Uninstalling the app removes all data from your device.")
        Para("If this policy changes, the new version will appear here. For any question, open Settings and tap Contact Us.")
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RatePage(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var rating by remember { mutableIntStateOf(0) }
    val labels = listOf("", "Not good", "Could be better", "It's okay", "Really good", "Loved it!")
    SubPage("Rate Us", onBack) {
        Spacer(Modifier.height(40.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Enjoying Sanatan Wallpaper?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Pal.text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("Tap a star to rate the app", fontSize = 16.sp, color = Pal.muted)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 1..5) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "$i star",
                        tint = if (i <= rating) Pal.accent else Pal.card2,
                        modifier = Modifier.size(52.dp).clip(CircleShape).clickable { rating = i }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(labels[rating], fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Pal.text, modifier = Modifier.height(24.dp))
            Spacer(Modifier.height(28.dp))
            PrimaryButton(
                "Submit",
                Modifier.fillMaxWidth().alpha(if (rating > 0) 1f else 0.35f)
            ) {
                if (rating > 0) {
                    if (rating >= 4) {
                        safeLaunch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ctx.packageName)))
                    } else {
                        Toast.makeText(ctx, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                    }
                    onBack()
                }
            }
        }
    }
}

private val faqs = listOf(
    "How do I download a wallpaper?" to "Tap any wallpaper and press Download. It is saved to your gallery, named like Hanuman Wallpaper 0.1.",
    "How do I like a wallpaper?" to "Tap the heart on a wallpaper. It will appear in the Liked tab.",
    "Do liked wallpapers work offline?" to "Yes. Liked wallpapers are stored on your phone, so the Liked tab works even without internet.",
    "Why are no new wallpapers loading?" to "The app needs internet to fetch the latest wallpapers. Connect to Wi-Fi or mobile data and tap Try Again. Cached wallpapers are shown until then.",
    "How does the home feed choose wallpapers for me?" to "Every category is mixed together. The more you open, like or download a category, the earlier its wallpapers appear. Other categories keep showing up too.",
    "How does search work?" to "Type a deity name or a tag. Search also understands small spelling mistakes, so \"mahadevv\" still finds Mahadev.",
    "How do I remove a search from history?" to "Tap the search bar and press the X next to any recent search. Use Clear all to remove everything.",
    "Where are my downloaded wallpapers?" to "Open your phone's Gallery or Photos app and look in the Sanatan Wallpaper album (Pictures folder).",
    "How do I set a downloaded wallpaper on my phone?" to "Open the image from your Gallery, tap the menu and choose Set as wallpaper.",
    "How can I report a problem or request a wallpaper?" to "Open Settings, tap Contact Us and send us an email. We read every message."
)

@Composable
private fun HelpPage(onBack: () -> Unit) {
    SubPage("Help", onBack) {
        faqs.forEachIndexed { index, (q, a) ->
            var open by remember { mutableStateOf(false) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (open) Pal.accentSoft else Pal.card)
                    .clickable { open = !open }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}.", color = Pal.accentDeep, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(q, modifier = Modifier.weight(1f), color = Pal.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(if (open) "\u2212" else "+", color = Pal.accentDeep, fontSize = 26.sp)
                }
                AnimatedVisibility(visible = open) {
                    Text(a, color = Pal.text.copy(alpha = 0.85f), fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ContactPage(onBack: () -> Unit) {
    val ctx = LocalContext.current
    SubPage("Contact Us", onBack) {
        Para("Have a question, a suggestion or a wallpaper request? We would love to hear from you.")
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Pal.card)) {
            SettingsRow("Email us", { Icon(Icons.Default.Email, null, tint = Pal.accentDeep, modifier = Modifier.size(24.dp)) }) {
                safeLaunch(
                    ctx,
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + AppInfo.CONTACT_EMAIL))
                        .putExtra(Intent.EXTRA_SUBJECT, "Sanatan Wallpaper")
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(AppInfo.CONTACT_EMAIL, color = Pal.muted, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 6.dp))
        Spacer(Modifier.height(16.dp))
    }
}

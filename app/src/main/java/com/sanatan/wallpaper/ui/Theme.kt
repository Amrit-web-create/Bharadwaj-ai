package com.sanatan.wallpaper.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

class Palette(
    val bg: Color,
    val card: Color,
    val card2: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,      // brand yellow
    val accentSoft: Color,  // very light yellow tint for surfaces
    val accentDeep: Color,  // readable golden for icons / small text
    val onAccent: Color,    // ink colour used on top of yellow
    val ink: Color,         // primary button background
    val onInk: Color,
    val inkIcon: Color,     // little circle behind the icon in primary buttons
    val onInkIcon: Color,
    val navBg: Color,
    val navFg: Color
)

val LightPalette = Palette(
    bg = Color(0xFFFFFEFA),
    card = Color(0xFFF8F5EA),
    card2 = Color(0xFFEDE7D3),
    text = Color(0xFF17120A),
    muted = Color(0xFF7A745F),
    line = Color(0xFFEAE3CB),
    accent = Color(0xFFFCCF27),
    accentSoft = Color(0xFFFFF4C9),
    accentDeep = Color(0xFF8A5E00),
    onAccent = Color(0xFF19120A),
    ink = Color(0xFF19120A),
    onInk = Color(0xFFFFF8E1),
    inkIcon = Color(0xFFFCCF27),
    onInkIcon = Color(0xFF19120A),
    navBg = Color(0xFF19120A),
    navFg = Color(0xFFDDD3B3)
)

val DarkPalette = Palette(
    bg = Color(0xFF0D0B07),
    card = Color(0xFF1A1710),
    card2 = Color(0xFF2B2619),
    text = Color(0xFFFFFAEB),
    muted = Color(0xFFA39B82),
    line = Color(0xFF2B2619),
    accent = Color(0xFFFCCF27),
    accentSoft = Color(0xFF2A230C),
    accentDeep = Color(0xFFFCCF27),
    onAccent = Color(0xFF19120A),
    ink = Color(0xFFFCCF27),
    onInk = Color(0xFF19120A),
    inkIcon = Color(0xFF19120A),
    onInkIcon = Color(0xFFFCCF27),
    navBg = Color(0xFF25211A),
    navFg = Color(0xFFB9B093)
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

object Pal {
    val bg: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.bg
    val card: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.card
    val card2: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.card2
    val text: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.text
    val muted: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.muted
    val line: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.line
    val accent: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.accent
    val accentSoft: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.accentSoft
    val accentDeep: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.accentDeep
    val onAccent: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.onAccent
    val ink: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
    val onInk: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.onInk
    val inkIcon: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.inkIcon
    val onInkIcon: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.onInkIcon
    val navBg: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.navBg
    val navFg: Color
        @Composable @ReadOnlyComposable get() = LocalPalette.current.navFg
}

@Composable
fun SanatanTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent, background = p.bg, onBackground = p.text,
            surface = p.bg, onSurface = p.text, surfaceVariant = p.card, onSurfaceVariant = p.muted
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent, background = p.bg, onBackground = p.text,
            surface = p.bg, onSurface = p.text, surfaceVariant = p.card, onSurfaceVariant = p.muted
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

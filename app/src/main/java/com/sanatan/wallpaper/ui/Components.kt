package com.sanatan.wallpaper.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sanatan.wallpaper.data.model.Wallpaper

@Composable
fun shimmerBrush(): Brush {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "x"
    )
    return Brush.linearGradient(
        listOf(Pal.card, Pal.card2, Pal.card),
        start = Offset(x - 400f, 0f),
        end = Offset(x, 400f)
    )
}

/** Small monochrome icons drawn with Canvas. */
@Composable
fun Glyph(kind: String, modifier: Modifier = Modifier.size(22.dp), color: Color = Pal.text) {
    Canvas(modifier) {
        val s = size.width / 24f
        val sw = 2f * s
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        fun line(a: Offset, b: Offset) = drawLine(color, a, b, strokeWidth = sw, cap = StrokeCap.Round)
        when (kind) {
            "download" -> {
                line(p(12f, 4f), p(12f, 15f))
                line(p(7f, 10.5f), p(12f, 15.5f))
                line(p(17f, 10.5f), p(12f, 15.5f))
                line(p(5f, 20f), p(19f, 20f))
            }
            "wa" -> {
                drawCircle(color, radius = 9f * s, center = p(12f, 12f), style = Stroke(sw))
                line(p(4.5f, 20f), p(6.5f, 15.5f))
                line(p(4.5f, 20f), p(9f, 18.5f))
                for (x in listOf(8.5f, 12f, 15.5f)) drawCircle(color, radius = 1.2f * s, center = p(x, 12f))
            }
            "tg" -> {
                line(p(3f, 11f), p(21f, 3f))
                line(p(21f, 3f), p(16f, 21f))
                line(p(16f, 21f), p(11f, 14f))
                line(p(11f, 14f), p(3f, 11f))
                line(p(21f, 3f), p(11f, 14f))
            }
            "copy" -> {
                drawRoundRect(color, topLeft = p(9f, 9f), size = Size(11f * s, 11f * s), cornerRadius = CornerRadius(2.5f * s), style = Stroke(sw))
                drawRoundRect(color, topLeft = p(4f, 4f), size = Size(11f * s, 11f * s), cornerRadius = CornerRadius(2.5f * s), style = Stroke(sw))
            }
            "dots" -> {
                for (x in listOf(5f, 12f, 19f)) drawCircle(color, radius = 2f * s, center = p(x, 12f))
            }
            "grid2", "grid4" -> {
                val cols = if (kind == "grid2") 2 else 4
                val rows = if (kind == "grid2") 2 else 3
                val pad = 3f
                val gap = if (cols == 2) 3f else 2f
                val cw = (24f - 2 * pad - gap * (cols - 1)) / cols
                val ch = (24f - 2 * pad - gap * (rows - 1)) / rows
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        drawRoundRect(
                            color,
                            topLeft = p(pad + c * (cw + gap), pad + r * (ch + gap)),
                            size = Size(cw * s, ch * s),
                            cornerRadius = CornerRadius(1.5f * s)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Image with a fixed decode size + stable memory-cache key so scrolling never reloads or blinks.
 * placeholderKey lets a bigger view show the already-cached smaller bitmap while loading.
 */
@Composable
fun WallImage(model: Any, memKey: String, placeholderKey: String?, w: Int, h: Int, modifier: Modifier) {
    val ctx = LocalContext.current
    val req = remember(model, memKey) {
        ImageRequest.Builder(ctx)
            .data(model)
            .memoryCacheKey(memKey)
            .placeholderMemoryCacheKey(placeholderKey)
            .size(w, h)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = req,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
fun WallpaperCell(id: String, model: Any, liked: Boolean, cols: Int, onClick: () -> Unit, onLike: () -> Unit) {
    val compact = cols >= 4
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.58f)
            .clip(RoundedCornerShape(if (compact) 10.dp else 18.dp))
            .background(Pal.card)
            .clickable(onClick = onClick)
    ) {
        if (compact) WallImage(model, "s_$id", "s_$id", 270, 465, Modifier.fillMaxSize())
        else WallImage(model, "g_$id", "g_$id", 540, 930, Modifier.fillMaxSize())
        if (!compact) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .clickable(onClick = onLike),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (liked) Pal.accent else Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
fun WallpaperGrid(
    items: List<Wallpaper>,
    vm: MainViewModel,
    liked: Set<String>,
    version: Int,
    cols: Int,
    state: LazyGridState = rememberLazyGridState(),
    onOpen: (Int) -> Unit
) {
    val gap = if (cols >= 4) 6.dp else 12.dp
    val side = if (cols >= 4) 8.dp else 14.dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(cols),
        state = state,
        contentPadding = PaddingValues(start = side, end = side, top = 6.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(items, key = { _, w -> w.id }) { i, w ->
            val model = remember(w.id, version) { vm.modelFor(w) }
            WallpaperCell(w.id, model, liked.contains(w.id), cols, { onOpen(i) }, { vm.toggleLike(w) })
        }
    }
}

@Composable
fun ShimmerGrid(cols: Int) {
    val b = shimmerBrush()
    val gap = if (cols >= 4) 6.dp else 12.dp
    val side = if (cols >= 4) 8.dp else 14.dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(cols),
        contentPadding = PaddingValues(start = side, end = side, top = 6.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize()
    ) {
        items(cols * 4) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.58f)
                    .clip(RoundedCornerShape(if (cols >= 4) 10.dp else 18.dp))
                    .background(b)
            )
        }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, glyph: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .height(56.dp)
            .clip(shape)
            .background(Pal.ink)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (glyph != null) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Pal.inkIcon),
                contentAlignment = Alignment.Center
            ) { Glyph(glyph, Modifier.size(18.dp), Pal.onInkIcon) }
            Spacer(Modifier.width(12.dp))
        }
        Text(text, color = Pal.onInk, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
    }
}

@Composable
fun OutlineButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .height(54.dp)
            .clip(shape)
            .border(1.dp, Pal.accent.copy(alpha = 0.55f), shape)
            .background(Pal.accentSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text, color = Pal.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1)
    }
}

@Composable
fun InfoBanner(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Pal.accentSoft)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Pal.accent))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Pal.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                color = Pal.accentDeep,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onAction).padding(start = 10.dp)
            )
        }
    }
}

@Composable
fun EmptyNote(title: String, message: String) {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Pal.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, fontSize = 15.sp, color = Pal.muted, textAlign = TextAlign.Center)
    }
}

/** Soft yellow "no signal" illustration drawn with Canvas (no image files needed). */
@Composable
fun ErrorIllustration(modifier: Modifier = Modifier) {
    val accent = Pal.accent
    val soft = Pal.accentSoft
    val ink = Pal.onAccent
    val ring = Pal.accent.copy(alpha = 0.40f)
    Canvas(modifier.size(200.dp)) {
        val w = size.width
        val cx = w / 2f
        val cy = w / 2f
        drawCircle(soft, radius = w * 0.50f, center = Offset(cx, cy))
        drawCircle(ring, radius = w * 0.40f, center = Offset(cx, cy), style = Stroke(width = w * 0.012f))
        drawCircle(accent, radius = w * 0.30f, center = Offset(cx, cy))
        // wifi arcs (centre point sits slightly below middle)
        val ox = cx
        val oy = cy + w * 0.09f
        val sw = w * 0.034f
        for (r in listOf(w * 0.09f, w * 0.16f, w * 0.23f)) {
            drawArc(
                ink, startAngle = 225f, sweepAngle = 90f, useCenter = false,
                topLeft = Offset(ox - r, oy - r), size = Size(r * 2f, r * 2f),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
        drawCircle(ink, radius = w * 0.026f, center = Offset(ox, oy))
        // slash
        drawLine(
            ink, Offset(cx - w * 0.17f, cy - w * 0.17f), Offset(cx + w * 0.17f, cy + w * 0.17f),
            strokeWidth = sw, cap = StrokeCap.Round
        )
        // little diamond sparkles (like the bindu in the app icon)
        fun diamond(x: Float, y: Float, r: Float, col: Color) {
            val d = Path().apply {
                moveTo(x, y - r); lineTo(x + r, y); lineTo(x, y + r); lineTo(x - r, y); close()
            }
            drawPath(d, col)
        }
        diamond(w * 0.82f, w * 0.20f, w * 0.045f, accent)
        diamond(w * 0.15f, w * 0.30f, w * 0.03f, ring)
        diamond(w * 0.86f, w * 0.78f, w * 0.028f, ring)
    }
}

@Composable
fun ErrorState(
    offline: Boolean,
    message: String,
    saved: List<Wallpaper>,
    vm: MainViewModel,
    version: Int,
    onRetry: () -> Unit,
    onViewSaved: () -> Unit,
    onOpenSaved: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ErrorIllustration()
            Spacer(Modifier.height(20.dp))
            Text(
                if (offline) "You're offline" else "Something went wrong",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif,
                color = Pal.text, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 15.sp, lineHeight = 22.sp, color = Pal.muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Try Again", Modifier.width(200.dp), onClick = onRetry)
        }
        if (saved.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your saved wallpapers", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif, color = Pal.text, modifier = Modifier.weight(1f)
                    )
                    Text(
                        "See all", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Pal.accentDeep,
                        modifier = Modifier.clip(CircleShape).clickable(onClick = onViewSaved).padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(saved, key = { _, w -> w.id }) { i, w ->
                        val model = remember(w.id, version) { vm.modelFor(w) }
                        Box(
                            Modifier
                                .width(104.dp)
                                .aspectRatio(0.58f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Pal.card)
                                .clickable { onOpenSaved(i) }
                        ) {
                            WallImage(model, "s_${w.id}", "s_${w.id}", 270, 465, Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

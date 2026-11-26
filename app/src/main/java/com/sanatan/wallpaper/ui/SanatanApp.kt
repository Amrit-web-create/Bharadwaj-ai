package com.sanatan.wallpaper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanatan.wallpaper.data.model.Wallpaper
import com.sanatan.wallpaper.util.ShareUtil
import kotlinx.coroutines.launch

data class DetailArgs(val list: List<Wallpaper>, val index: Int)

@Composable
fun SanatanApp(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<DetailArgs?>(null) }
    var lastDetail by remember { mutableStateOf<DetailArgs?>(null) }
    var page by remember { mutableStateOf<String?>(null) }
    var lastPage by remember { mutableStateOf("about") }
    val homeGrid = rememberLazyGridState()
    val likedGrid = rememberLazyGridState()

    val onOpen: (List<Wallpaper>, Int) -> Unit = { list, i ->
        val a = DetailArgs(list, i)
        lastDetail = a
        detail = a
    }

    BackHandler(enabled = tab != 0) { tab = 0 }

    Box(Modifier.fillMaxSize().background(Pal.bg)) {
        Scaffold(
            containerColor = Pal.bg,
            contentColor = Pal.text,
            bottomBar = { PillNav(tab) { tab = it } }
        ) { pad ->
            when (tab) {
                0 -> HomeScreen(vm, pad, homeGrid, onOpen) { tab = 1 }
                1 -> LikedScreen(vm, pad, likedGrid, onOpen)
                else -> SettingsTab(vm, pad) { lastPage = it; page = it }
            }
        }

        AnimatedVisibility(
            visible = detail != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 8 },
            exit = fadeOut(tween(160))
        ) {
            lastDetail?.let { d -> DetailScreen(vm, d) { detail = null } }
        }

        AnimatedVisibility(
            visible = page != null,
            enter = slideInHorizontally(tween(260)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(160))
        ) {
            SettingsPageOverlay(page ?: lastPage) { page = null }
        }

        SmartSheetHost(vm)
    }
}

// ---------- expanding pill bottom navigation ----------
@Composable
private fun PillNav(tab: Int, onTab: (Int) -> Unit) {
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 6.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .shadow(14.dp, CircleShape, ambientColor = Color(0x33000000), spotColor = Color(0x55000000))
                .clip(CircleShape)
                .background(Pal.navBg)
                .border(1.dp, Pal.accent.copy(alpha = 0.35f), CircleShape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavItem("Home", Icons.Default.Home, tab == 0) { onTab(0) }
            NavItem("Liked", Icons.Default.Favorite, tab == 1) { onTab(1) }
            NavItem("Settings", Icons.Default.Settings, tab == 2) { onTab(2) }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Pal.accent else Color.Transparent, label = "navbg")
    val fg by animateColorAsState(if (selected) Pal.onAccent else Pal.navFg, label = "navfg")
    Row(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .animateContentSize(tween(220))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(24.dp))
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(tween(220)) + fadeIn(tween(220)),
            exit = shrinkHorizontally(tween(180)) + fadeOut(tween(120))
        ) {
            Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

// ---------- detail: half-screen preview + share + download ----------
@Composable
private fun ShareItem(label: String, glyph: String?, icon: ImageVector?, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(58.dp).clip(CircleShape).background(Pal.accentSoft).border(1.dp, Pal.accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (glyph != null) Glyph(glyph, Modifier.size(26.dp), Pal.accentDeep)
            else if (icon != null) Icon(icon, null, tint = Pal.accentDeep, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Pal.accentDeep, maxLines = 1)
    }
}

@Composable
private fun DetailScreen(vm: MainViewModel, args: DetailArgs, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val liked by vm.liked.collectAsStateWithLifecycle()
    val ver by vm.savedVersion.collectAsStateWithLifecycle()
    val last = (args.list.size - 1).coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = args.index.coerceIn(0, last)) { args.list.size }
    var fullIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler { onClose() }
    LaunchedEffect(pager.settledPage) {
        args.list.getOrNull(pager.settledPage)?.let { vm.onOpened(it) }
    }

    Box(Modifier.fillMaxSize().background(Pal.bg).pointerInput(Unit) { detectTapGestures { } }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Pal.accentSoft).border(1.dp, Pal.accent.copy(alpha = 0.5f), CircleShape).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Pal.accentDeep) }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val cardW = (maxHeight * 0.56f).coerceAtMost(maxWidth - 64.dp)
                val side = (maxWidth - cardW) / 2
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = side),
                    pageSpacing = 14.dp,
                    key = { args.list[it].id }
                ) { p ->
                    val w = args.list[p]
                    val model = remember(w.id, ver) { vm.modelFor(w) }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Pal.card)
                            .clickable { fullIndex = p }
                    ) {
                        WallImage(model, "d_${w.id}", "g_${w.id}", 720, 1280, Modifier.fillMaxSize())
                    }
                }
            }

            val current = args.list.getOrNull(pager.currentPage)
            if (current != null) {
                val isLiked = liked.contains(current.id)
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isLiked) Pal.accent else Pal.accentSoft)
                                .border(1.dp, Pal.accent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                                .clickable { vm.toggleLike(current) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) Pal.onAccent else Pal.accentDeep,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        PrimaryButton("Download", Modifier.weight(1f), glyph = "download") { vm.download(current) }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Share", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Pal.accentDeep, modifier = Modifier.padding(start = 4.dp))
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ShareItem("WhatsApp", "wa", null) { ShareUtil.share(ctx, "com.whatsapp") }
                        ShareItem("Telegram", "tg", null) { ShareUtil.share(ctx, "org.telegram.messenger") }
                        ShareItem("Copy link", "copy", null) { ShareUtil.copyLink(ctx) }
                        ShareItem("More", "dots", null) { ShareUtil.share(ctx, null) }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlineButton("View full screen", Modifier.fillMaxWidth()) { fullIndex = pager.currentPage }
                }
            }
        }

        AnimatedVisibility(visible = fullIndex != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(150))) {
            FullScreenViewer(vm, args.list, fullIndex ?: 0) { idx ->
                scope.launch { pager.scrollToPage(idx) }
                fullIndex = null
            }
        }
    }
}

@Composable
private fun FullScreenViewer(vm: MainViewModel, list: List<Wallpaper>, start: Int, onClose: (Int) -> Unit) {
    val ver by vm.savedVersion.collectAsStateWithLifecycle()
    val st = rememberPagerState(initialPage = start.coerceIn(0, (list.size - 1).coerceAtLeast(0))) { list.size }
    BackHandler { onClose(st.currentPage) }
    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures { } }) {
        HorizontalPager(state = st, modifier = Modifier.fillMaxSize(), key = { list[it].id }) { p ->
            val w = list[p]
            val model = remember(w.id, ver) { vm.modelFor(w) }
            WallImage(model, "b_${w.id}", "d_${w.id}", 1080, 1920, Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
                .clickable { onClose(st.currentPage) },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
    }
}

// ---------- smart bottom sheet popups ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartSheetHost(vm: MainViewModel) {
    val sheet by vm.sheet.collectAsStateWithLifecycle()
    val s = sheet ?: return
    ModalBottomSheet(
        onDismissRequest = { vm.dismissSheet() },
        containerColor = Pal.bg,
        contentColor = Pal.text,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Pal.card2) }
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (s) {
                is SheetUi.Working -> {
                    CircularProgressIndicator(color = Pal.accent, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                    SheetText(s.title, s.message)
                }
                is SheetUi.Success -> {
                    SheetBadge(Icons.Default.Check)
                    SheetText("Download complete", s.name + " is saved to your gallery")
                    SheetButton("Done") { vm.dismissSheet() }
                }
                is SheetUi.Failure -> {
                    SheetBadge(Icons.Default.Warning)
                    SheetText("Couldn't download", s.message)
                    SheetButton("Okay") { vm.dismissSheet() }
                }
                is SheetUi.Offline -> {
                    SheetBadge(Icons.Default.Warning)
                    SheetText("You're offline", "Connect to the internet to download this wallpaper. Liked and local wallpapers can be downloaded anytime.")
                    SheetButton("Got it") { vm.dismissSheet() }
                }
            }
        }
    }
}

@Composable
private fun SheetBadge(icon: ImageVector) {
    Box(Modifier.size(68.dp).clip(CircleShape).background(Pal.accent), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Pal.onAccent, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun SheetText(title: String, message: String) {
    Spacer(Modifier.height(16.dp))
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Pal.text, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(message, fontSize = 15.sp, color = Pal.muted, textAlign = TextAlign.Center, lineHeight = 20.sp)
}

@Composable
private fun SheetButton(text: String, onClick: () -> Unit) {
    Spacer(Modifier.height(22.dp))
    PrimaryButton(text, Modifier.fillMaxWidth(), onClick = onClick)
}

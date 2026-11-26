package com.sanatan.wallpaper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanatan.wallpaper.data.model.Wallpaper
import com.sanatan.wallpaper.data.model.prettyCategory
import kotlinx.coroutines.launch

@Composable
private fun SearchBar(
    query: String,
    onChange: (String) -> Unit,
    onFocus: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Pal.card)
            .border(1.dp, Pal.line, RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Pal.muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Search wallpapers", color = Pal.muted, fontSize = 17.sp, fontFamily = FontFamily.Serif, maxLines = 1)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Pal.text, fontSize = 18.sp, fontFamily = FontFamily.Serif),
                cursorBrush = SolidColor(Pal.text),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { onFocus(it.isFocused) }
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Default.Close, "Clear", tint = Pal.muted,
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onClear).padding(2.dp)
            )
        }
    }
}

/** 2 per row / 4 per row switch */
@Composable
fun ViewToggle(cols: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Pal.card).border(1.dp, Pal.line, RoundedCornerShape(16.dp)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (c in listOf(2, 4)) {
            val sel = cols == c
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (sel) Pal.accent else Color.Transparent)
                    .clickable { onChange(c) },
                contentAlignment = Alignment.Center
            ) {
                Glyph(if (c == 2) "grid2" else "grid4", Modifier.size(22.dp), if (sel) Pal.onAccent else Pal.muted)
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (selected) Pal.accent else Pal.card)
            .border(1.dp, if (selected) Pal.accent else Pal.line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(text, color = if (selected) Pal.onAccent else Pal.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun CategoryChips(cats: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Chip("For You", selected == null) { onSelect(null) } }
        items(cats, key = { it }) { c -> Chip(prettyCategory(c), selected.equals(c, true)) { onSelect(c) } }
    }
}

@Composable
private fun SearchHome(
    history: List<String>,
    suggestions: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent searches", fontWeight = FontWeight.Bold, color = Pal.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    Text("Clear all", color = Pal.muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onClear))
                }
            }
        }
        if (history.isEmpty()) {
            item { Text("Your searches will appear here", color = Pal.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp)) }
        }
        items(history, key = { it }) { h ->
            Row(
                Modifier.fillMaxWidth().clickable { onPick(h) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Pal.muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(h, color = Pal.text, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(
                    Icons.Default.Close, "Remove", tint = Pal.muted,
                    modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onRemove(h) }.padding(4.dp)
                )
            }
        }
        if (suggestions.isNotEmpty()) {
            item {
                Spacer(Modifier.height(18.dp))
                Text("Suggested for you", fontWeight = FontWeight.Bold, color = Pal.text, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions, key = { it }) { s -> Chip(prettyCategory(s), false) { onPick(s) } }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    vm: MainViewModel,
    pad: PaddingValues,
    gridState: LazyGridState,
    onOpen: (List<Wallpaper>, Int) -> Unit,
    onGoLiked: () -> Unit
) {
    val feed by vm.feed.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val load by vm.loadState.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    val failed by vm.syncFailed.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val liked by vm.liked.collectAsStateWithLifecycle()
    val savedList by vm.saved.collectAsStateWithLifecycle()
    val ver by vm.savedVersion.collectAsStateWithLifecycle()
    val cols by vm.gridCols.collectAsStateWithLifecycle()

    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val searching = focused || query.isNotBlank()
    val chipCats = remember(cats) { cats.filter { !it.equals("Local", true) } }

    BackHandler(enabled = searching) {
        vm.setQuery("")
        focusManager.clearFocus()
    }

    val shown = remember(feed, selected) {
        val sel = selected
        if (sel == null) feed else feed.filter { it.category.equals(sel, true) }
    }

    Column(Modifier.fillMaxSize().padding(pad)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sanatan", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Pal.text)
            Text(" Wallpaper", fontSize = 24.sp, fontWeight = FontWeight.Light, color = Pal.accentDeep)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SearchBar(
                query = query,
                onChange = vm::setQuery,
                onFocus = { focused = it },
                onSubmit = { vm.commitSearch(query); focusManager.clearFocus() },
                onClear = { vm.setQuery("") },
                modifier = Modifier.weight(1f)
            )
            ViewToggle(cols) { vm.setGridCols(it) }
        }
        AnimatedVisibility(visible = !online) { InfoBanner("Offline mode - showing cached and liked wallpapers") }
        AnimatedVisibility(visible = online && failed) { InfoBanner("Couldn't reach server", "Retry") { vm.refresh() } }
        if (!searching && feed.isNotEmpty()) {
            CategoryChips(chipCats, selected) {
                selected = it
                if (it != null) vm.onCategoryView(it)
                vm.rerank()
                scope.launch { gridState.scrollToItem(0) }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                searching && query.isBlank() -> SearchHome(
                    history = history,
                    suggestions = chipCats.take(6),
                    onPick = { vm.setQuery(it); vm.commitSearch(it); focusManager.clearFocus() },
                    onRemove = vm::removeHistory,
                    onClear = vm::clearHistory
                )
                query.isNotBlank() -> {
                    if (results.isEmpty()) EmptyNote("Nothing found", "Try another spelling or a different name")
                    else WallpaperGrid(results, vm, liked, ver, cols) { i -> vm.commitSearch(query); onOpen(results, i) }
                }
                feed.isNotEmpty() -> WallpaperGrid(shown, vm, liked, ver, cols, gridState) { i -> onOpen(shown, i) }
                load is LoadState.Error -> ErrorState(
                    offline = !online,
                    message = (load as LoadState.Error).msg,
                    saved = savedList,
                    vm = vm,
                    version = ver,
                    onRetry = { vm.refresh() },
                    onViewSaved = onGoLiked,
                    onOpenSaved = { i -> onOpen(savedList, i) }
                )
                load is LoadState.Loading -> ShimmerGrid(cols)
                else -> EmptyNote("No wallpapers yet", "New wallpapers are on the way")
            }
        }
    }
}

@Composable
fun LikedScreen(vm: MainViewModel, pad: PaddingValues, gridState: LazyGridState, onOpen: (List<Wallpaper>, Int) -> Unit) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val liked by vm.liked.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    val ver by vm.savedVersion.collectAsStateWithLifecycle()
    val cols by vm.gridCols.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(pad)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Liked", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Pal.text)
                Text("Available offline", fontSize = 13.sp, color = Pal.muted)
            }
            ViewToggle(cols) { vm.setGridCols(it) }
        }
        AnimatedVisibility(visible = !online) { InfoBanner("Offline mode - your liked wallpapers are ready") }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (saved.isEmpty()) {
                EmptyNote("Nothing liked yet", "Tap the heart on any wallpaper and it will be kept here for offline use")
            } else {
                WallpaperGrid(saved, vm, liked, ver, cols, gridState) { i -> onOpen(saved, i) }
            }
        }
    }
}

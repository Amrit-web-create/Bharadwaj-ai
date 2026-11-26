package com.sanatan.wallpaper.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.sanatan.wallpaper.data.local.CatalogCache
import com.sanatan.wallpaper.data.local.SavedStore
import com.sanatan.wallpaper.data.local.UserPrefs
import com.sanatan.wallpaper.data.model.Wallpaper
import com.sanatan.wallpaper.data.model.downloadBaseName
import com.sanatan.wallpaper.data.remote.WallpaperRemoteLoader
import com.sanatan.wallpaper.domain.ExploreFeedEngine
import com.sanatan.wallpaper.domain.SmartSearchEngine
import com.sanatan.wallpaper.util.Downloader
import com.sanatan.wallpaper.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LoadState {
    data object Loading : LoadState
    data object Ready : LoadState
    data class Error(val msg: String) : LoadState
}

sealed interface SheetUi {
    data class Working(val title: String, val message: String) : SheetUi
    data class Success(val name: String) : SheetUi
    data class Failure(val message: String) : SheetUi
    data object Offline : SheetUi
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val prefs = UserPrefs(ctx)
    private val savedStore = SavedStore(ctx)
    private val cache = CatalogCache(ctx)
    private val network = NetworkMonitor(ctx)
    val online: StateFlow<Boolean> = network.online
    private val seed = System.currentTimeMillis()

    private var local: List<Wallpaper> = emptyList()
    private var remote: List<Wallpaper> = emptyList()
    private var lastFailed = false
    private var refreshJob: Job? = null
    private var prefetchJob: Job? = null

    private val _all = MutableStateFlow<List<Wallpaper>>(emptyList())
    private val _feed = MutableStateFlow<List<Wallpaper>>(emptyList())
    val feed: StateFlow<List<Wallpaper>> = _feed
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories
    private val _load = MutableStateFlow<LoadState>(LoadState.Loading)
    val loadState: StateFlow<LoadState> = _load
    private val _syncFailed = MutableStateFlow(false)
    val syncFailed: StateFlow<Boolean> = _syncFailed
    private val _liked = MutableStateFlow(prefs.likedIds())
    val liked: StateFlow<Set<String>> = _liked
    private val _saved = MutableStateFlow(savedStore.load())
    val saved: StateFlow<List<Wallpaper>> = _saved
    private val _savedVersion = MutableStateFlow(0)
    val savedVersion: StateFlow<Int> = _savedVersion
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _history = MutableStateFlow(prefs.history())
    val history: StateFlow<List<String>> = _history
    private val _sheet = MutableStateFlow<SheetUi?>(null)
    val sheet: StateFlow<SheetUi?> = _sheet
    private val _gridCols = MutableStateFlow(prefs.gridCols())
    val gridCols: StateFlow<Int> = _gridCols
    private val _theme = MutableStateFlow(prefs.themeMode())
    val themeMode: StateFlow<Int> = _theme

    val results: StateFlow<List<Wallpaper>> =
        combine(_query.debounce(150), _all) { q, a -> q to a }
            .mapLatest { (q, a) -> SmartSearchEngine.search(q, a) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            local = withContext(Dispatchers.IO) { loadLocal() }
            val raw = withContext(Dispatchers.IO) { cache.read() }
            if (raw != null) {
                remote = runCatching { WallpaperRemoteLoader.parse(raw) }.getOrDefault(emptyList())
            }
            publish()
            if (_all.value.isNotEmpty()) _load.value = LoadState.Ready
            refresh()
        }
        viewModelScope.launch {
            online.drop(1).collect { isOn ->
                if (isOn) {
                    if (remote.isEmpty() || lastFailed) refresh()
                    syncSaved()
                }
            }
        }
    }

    // ---------- settings ----------
    fun setGridCols(n: Int) { prefs.setGridCols(n); _gridCols.value = n }
    fun setTheme(m: Int) { prefs.setThemeMode(m); _theme.value = m }

    // ---------- catalog ----------
    private fun publish() {
        _all.value = (local + remote).distinctBy { it.id }
        rerank()
    }

    /** Re-rank the explore feed using the latest interest scores. */
    fun rerank() {
        val cat = prefs.catInterest()
        val tag = prefs.tagInterest()
        val list = _all.value
        _feed.value = ExploreFeedEngine.build(list, cat, tag, seed)
        _categories.value = list.map { it.category.trim() }
            .distinctBy { it.lowercase() }
            .sortedByDescending { cat[it.lowercase()] ?: 0.0 }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            if (_all.value.isEmpty()) _load.value = LoadState.Loading
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = WallpaperRemoteLoader.fetchRaw()
                    val list = WallpaperRemoteLoader.parse(raw)
                    if (list.isEmpty()) throw IllegalStateException("empty catalog")
                    cache.write(raw)
                    list
                }
            }
            val list = res.getOrNull()
            if (list != null) {
                remote = list
                lastFailed = false
                _syncFailed.value = false
                publish()
                _load.value = LoadState.Ready
                startPrefetch()
                syncSaved()
            } else {
                lastFailed = true
                _syncFailed.value = true
                _load.value = if (_all.value.isEmpty()) {
                    LoadState.Error(
                        if (!online.value) "No internet connection and no wallpapers are cached yet. Turn on the internet and try again."
                        else "We couldn't load wallpapers right now. Please try again in a moment."
                    )
                } else LoadState.Ready
            }
        }
    }

    /** Images in assets/localwallpaper. Category comes from the file name (mahadev_1.jpg -> Mahadev). */
    private fun loadLocal(): List<Wallpaper> {
        val names = runCatching { ctx.assets.list("localwallpaper")?.toList() }.getOrNull() ?: emptyList()
        val ok = setOf("jpg", "jpeg", "png", "webp")
        return names.filter { it.substringAfterLast('.', "").lowercase() in ok }.sorted().map { n ->
            val stem = n.substringBeforeLast('.')
            val cat = Regex("^[A-Za-z]{3,}").find(stem)?.value?.lowercase()
                ?.replaceFirstChar { c -> c.uppercase() } ?: "Local"
            Wallpaper(
                id = "local_$n",
                title = stem.replace(Regex("[_\\-]+"), " ").trim(),
                category = cat,
                source = "Local",
                tags = listOf("local", cat.lowercase()),
                url = Downloader.ASSET_PREFIX + "localwallpaper/" + n,
                isLocal = true
            )
        }
    }

    /** Slow background preloading: a few images at a time, only when online. */
    private fun startPrefetch() {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            val loader = ctx.imageLoader
            val targets = _feed.value.filter { !it.isLocal }.take(120)
            for (w in targets) {
                if (!online.value) online.first { it }
                val req = ImageRequest.Builder(ctx)
                    .data(w.url)
                    .size(540, 930)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
                runCatching { loader.execute(req) }
                delay(350)
            }
        }
    }

    // ---------- images ----------
    fun modelFor(w: Wallpaper): Any {
        if (w.isLocal || !_liked.value.contains(w.id)) return w.url
        val f = savedStore.fileFor(w)
        return if (f.exists() && f.length() > 0) f else w.url
    }

    // ---------- interest ----------
    fun onOpened(w: Wallpaper) = prefs.bump(w, 1.0)
    fun onCategoryView(category: String) = prefs.bumpCategory(category, 1.0)

    // ---------- like / saved ----------
    fun toggleLike(w: Wallpaper) {
        if (_liked.value.contains(w.id)) {
            prefs.setLiked(w.id, false)
            prefs.bump(w, -2.0)
            _saved.value = savedStore.remove(w)
        } else {
            prefs.setLiked(w.id, true)
            prefs.bump(w, 3.0)
            _saved.value = savedStore.add(w)
            viewModelScope.launch(Dispatchers.IO) {
                if (!w.isLocal && online.value) {
                    runCatching { Downloader.cacheToFile(ctx, w, savedStore.fileFor(w)) }
                }
                _savedVersion.value = _savedVersion.value + 1
            }
        }
        _liked.value = prefs.likedIds()
        _savedVersion.value = _savedVersion.value + 1
    }

    private fun syncSaved() {
        viewModelScope.launch(Dispatchers.IO) {
            for (w in savedStore.load()) {
                if (w.isLocal || savedStore.hasFile(w)) continue
                if (!online.value) break
                runCatching { Downloader.cacheToFile(ctx, w, savedStore.fileFor(w)) }
            }
            _savedVersion.value = _savedVersion.value + 1
        }
    }

    // ---------- download ----------
    fun download(w: Wallpaper) {
        val haveCopy = w.isLocal || savedStore.hasFile(w)
        if (!online.value && !haveCopy) {
            _sheet.value = SheetUi.Offline
            return
        }
        _sheet.value = SheetUi.Working("Downloading", "Saving to your gallery")
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val base = downloadBaseName(w)
                    val n = prefs.downloadCount(base) + 1
                    val name = "$base 0.$n"
                    Downloader.saveToGallery(ctx, w, savedStore.fileFor(w), name)
                    prefs.setDownloadCount(base, n)
                    name
                }
            }
            val name = r.getOrNull()
            if (name != null) {
                prefs.bump(w, 4.0)
                _sheet.value = SheetUi.Success(name)
            } else {
                _sheet.value = SheetUi.Failure("Please check your internet connection and try again.")
            }
        }
    }

    // ---------- search ----------
    fun setQuery(q: String) { _query.value = q }
    fun commitSearch(q: String) { _history.value = prefs.addHistory(q) }
    fun removeHistory(q: String) { _history.value = prefs.removeHistory(q) }
    fun clearHistory() { prefs.clearHistory(); _history.value = emptyList() }

    fun dismissSheet() { _sheet.value = null }
}

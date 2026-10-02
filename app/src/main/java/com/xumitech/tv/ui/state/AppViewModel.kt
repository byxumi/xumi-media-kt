package com.xumitech.tv.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xumitech.tv.data.ApiException
import com.xumitech.tv.data.AuthStore
import com.xumitech.tv.data.DoubanItem
import com.xumitech.tv.data.Favorite
import com.xumitech.tv.data.Following
import com.xumitech.tv.data.MoonApi
import com.xumitech.tv.data.PlayRecord
import com.xumitech.tv.data.SearchResult
import com.xumitech.tv.data.ServerConfig
import com.xumitech.tv.data.Source
import com.xumitech.tv.data.TodayUpdatedRecord
import com.xumitech.tv.data.VideoGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder

// ================= 领域状态 =================

data class HomeSection(
    val title: String,
    val kind: String,
    val items: List<DoubanItem>,
)

sealed interface AuthState {
    data object Unknown : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val username: String, val isAdmin: Boolean) : AuthState
}

data class AppUiState(
    val auth: AuthState = AuthState.Unknown,
    val server: String = MoonApi.DEFAULT_SERVER,
    val themeMode: String = "system",
    val siteName: String = "",
    val serverVersion: String = "",
    val sources: List<Source> = emptyList(),
    val homeSections: List<HomeSection> = emptyList(),
    val homeLoading: Boolean = false,
    val homeError: String? = null,
    val todayUpdated: TodayUpdatedRecord? = null,
    val favorites: Map<String, Favorite> = emptyMap(),
    val playRecords: Map<String, PlayRecord> = emptyMap(),
    val followings: Map<String, Following> = emptyMap(),
    val searchHistory: List<String> = emptyList(),
    // 搜索页
    val searching: Boolean = false,
    val searchResults: List<SearchResult> = emptyList(),
    val searchError: String? = null,
    // 详情页
    val detailLoading: Boolean = false,
    val detailError: String? = null,
    val detailGroups: List<VideoGroup> = emptyList(),
    // 发现页
    val discoverLoading: Boolean = false,
    val discoverError: String? = null,
    val discoverItems: List<DoubanItem> = emptyList(),
    // 管理页
    val isAdmin: Boolean = false,
    val adminConfig: Map<String, Any?> = emptyMap(),
    val adminLoading: Boolean = false,
    val adminError: String? = null,
    // 消息
    val toast: String? = null,
) {
    val recentRecords: List<PlayRecord>
        get() = playRecords.values
            .filter { it.playTime > 5 && it.totalTime > 0 }
            .sortedByDescending { it.saveTime }
            .take(12)
}

// ================= ViewModel =================

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = AuthStore(app)
    private val _ui = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = _ui.asStateFlow()

    init {
        val server = store.server
        val cookie = store.cookie
        MoonApi.configure(server, cookie)
        _ui.update {
            it.copy(
                server = server,
                themeMode = store.themeMode,
                auth = if (cookie != null) AuthState.LoggedIn(parseUsername(cookie), parseRole(cookie)) else AuthState.LoggedOut,
            )
        }
        MoonApi.onAuthChanged = { persistCookie() }
    }

    private fun parseUsername(cookie: String): String {
        return try {
            val json = decode2(cookie)
            val m = Regex("\"username\"\\s*:\\s*\"([^\"]+)\"").find(json)
            m?.groupValues?.get(1) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseRole(cookie: String): Boolean {
        return try {
            val json = decode2(cookie)
            val m = Regex("\"role\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val role = m?.groupValues?.get(1) ?: ""
            role == "owner" || role == "admin"
        } catch (_: Exception) {
            false
        }
    }

    private fun decode2(s: String): String {
        var r = s
        repeat(2) { r = URLDecoder.decode(r, "UTF-8") }
        return r
    }

    private fun persistCookie() {
        val c = MoonApi.currentCookie()
        store.cookie = c
        _ui.update {
            it.copy(
                auth = if (c != null) AuthState.LoggedIn(parseUsername(c), parseRole(c)) else AuthState.LoggedOut,
            )
        }
    }

    fun setToast(msg: String?) {
        _ui.update { it.copy(toast = msg) }
    }

    // ---------------- 认证 ----------------
    fun login(username: String, password: String, onDone: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                MoonApi.login(username.trim(), password)
                persistCookie()
                loadAll()
                onDone(true, null)
            } catch (e: ApiException) {
                onDone(false, friendlyError(e))
            } catch (e: Exception) {
                onDone(false, e.message ?: "网络错误")
            }
        }
    }

    fun register(username: String, password: String, onDone: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                MoonApi.register(username.trim(), password)
                persistCookie()
                loadAll()
                onDone(true, null)
            } catch (e: ApiException) {
                onDone(false, friendlyError(e))
            } catch (e: Exception) {
                onDone(false, e.message ?: "网络错误")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            MoonApi.logout()
            store.clear()
            _ui.update {
                it.copy(
                    auth = AuthState.LoggedOut,
                    homeSections = emptyList(),
                    favorites = emptyMap(),
                    playRecords = emptyMap(),
                    followings = emptyMap(),
                    searchHistory = emptyList(),
                )
            }
        }
    }

    fun changeThemeMode(mode: String) {
        store.themeMode = mode
        _ui.update { it.copy(themeMode = mode) }
    }

    fun setServer(server: String) {
        val s = server.trim().trimEnd('/')
        store.server = s
        MoonApi.configure(s, null)
        _ui.update { it.copy(server = s, auth = AuthState.LoggedOut) }
    }

    // ---------------- 数据加载 ----------------
    fun loadAll() {
        viewModelScope.launch {
            try {
                val cfg = MoonApi.getServerConfig()
                _ui.update { it.copy(siteName = cfg.siteName, serverVersion = cfg.version) }
            } catch (_: Exception) {
            }
            loadFavorites()
            loadPlayRecords()
            loadFollowings()
            loadSearchHistory()
            loadHome()
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _ui.update { it.copy(homeLoading = true, homeError = null) }
            try {
                val sources = MoonApi.getSources()
                val kinds = listOf("movie", "tv", "anime")
                val sections = mutableListOf<HomeSection>()
                for (k in kinds) {
                    val items = try {
                        MoonApi.getDoubanRecommends(kind = k, limit = 12)
                    } catch (_: Exception) {
                        emptyList()
                    }
                    if (items.isNotEmpty()) {
                        sections += HomeSection(
                            title = when (k) {
                                "movie" -> "热门电影"
                                "tv" -> "热门剧集"
                                else -> "热门动漫"
                            },
                            kind = k,
                            items = items,
                        )
                    }
                }
                val today = try { MoonApi.getTodayUpdated() } catch (_: Exception) { null }
                _ui.update { it.copy(sources = sources, homeSections = sections, todayUpdated = today, homeLoading = false) }
            } catch (e: Exception) {
                _ui.update { it.copy(homeLoading = false, homeError = e.message ?: "加载失败") }
            }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                val m = MoonApi.getFavorites()
                _ui.update { it.copy(favorites = m) }
            } catch (_: Exception) {
            }
        }
    }

    fun loadPlayRecords() {
        viewModelScope.launch {
            try {
                val m = MoonApi.getPlayRecords()
                _ui.update { it.copy(playRecords = m) }
            } catch (_: Exception) {
            }
        }
    }

    fun loadFollowings() {
        viewModelScope.launch {
            try {
                val m = MoonApi.getFollowings()
                _ui.update { it.copy(followings = m) }
            } catch (_: Exception) {
            }
        }
    }

    fun loadSearchHistory() {
        viewModelScope.launch {
            try {
                val l = MoonApi.getSearchHistory()
                _ui.update { it.copy(searchHistory = l) }
            } catch (_: Exception) {
            }
        }
    }

    // ---------------- 收藏 / 追更 / 记录 ----------------
    fun toggleFavorite(item: SearchResult) {
        val key = item.key
        _ui.update { it.copy(favorites = it.favorites.toMutableMap().apply {
            if (containsKey(key)) remove(key) else put(key, Favorite(
                id = item.id,
                source = item.source,
                title = item.title,
                sourceName = item.sourceName,
                cover = item.poster,
                year = item.year,
                saveTime = System.currentTimeMillis(),
                searchTitle = item.title,
            ))
        }) }
        viewModelScope.launch {
            try {
                val cur = _ui.value.favorites
                if (cur.containsKey(key)) {
                    MoonApi.saveFavorite(key, cur[key]!!)
                } else {
                    MoonApi.deleteFavorite(key)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun isFavorite(item: SearchResult): Boolean = _ui.value.favorites.containsKey(item.key)

    fun toggleFollowing(item: SearchResult, totalEpisodes: Int = 0) {
        val key = item.key
        _ui.update { it.copy(followings = it.followings.toMutableMap().apply {
            if (containsKey(key)) remove(key) else put(key, Following(
                id = item.id,
                source = item.source,
                sourceName = item.sourceName,
                totalEpisodes = totalEpisodes,
                watchedEpisodes = 0,
                title = item.title,
                year = item.year,
                cover = item.poster,
                saveTime = System.currentTimeMillis(),
                searchTitle = item.title,
            ))
        }) }
        viewModelScope.launch {
            try {
                val cur = _ui.value.followings
                if (cur.containsKey(key)) {
                    MoonApi.saveFollowing(key, cur[key]!!)
                } else {
                    MoonApi.deleteFollowing(key)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun isFollowing(item: SearchResult): Boolean = _ui.value.followings.containsKey(item.key)

    fun savePlayRecord(item: SearchResult, index: Int, playTime: Int, totalTime: Int) {
        val key = item.key
        val record = PlayRecord(
            id = item.id,
            source = item.source,
            title = item.title,
            sourceName = item.sourceName,
            cover = item.poster,
            year = item.year,
            index = index,
            totalEpisodes = item.episodes.size,
            playTime = playTime,
            totalTime = totalTime,
            saveTime = System.currentTimeMillis(),
            searchTitle = item.title,
        )
        _ui.update { it.copy(playRecords = it.playRecords.toMutableMap().apply { put(key, record) }) }
        viewModelScope.launch {
            try {
                MoonApi.savePlayRecord(key, record)
            } catch (_: Exception) {
            }
        }
    }

    fun removePlayRecord(key: String) {
        _ui.update { it.copy(playRecords = it.playRecords.toMutableMap().apply { remove(key) }) }
        viewModelScope.launch {
            try {
                MoonApi.deletePlayRecord(key)
            } catch (_: Exception) {
            }
        }
    }

    /** 追更观看进度同步(看完第 i 集时更新 watchedEpisodes)。 */
    fun syncFollowingProgress(item: SearchResult, episodeIndex: Int) {
        val key = item.key
        val cur = _ui.value.followings[key] ?: return
        val watched = maxOf(cur.watchedEpisodes, episodeIndex + 1)
        if (watched == cur.watchedEpisodes) return
        val updated = cur.copy(watchedEpisodes = watched)
        _ui.update { it.copy(followings = it.followings.toMutableMap().apply { put(key, updated) }) }
        viewModelScope.launch {
            try {
                MoonApi.saveFollowing(key, updated)
            } catch (_: Exception) {
            }
        }
    }

    // ---------------- 搜索 ----------------
    fun searchSuggestions(q: String, onDone: (List<String>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                onDone(MoonApi.searchSuggestions(q))
            } catch (_: Exception) {
                onDone(emptyList())
            }
        }
    }

    fun search(q: String) {
        viewModelScope.launch {
            _ui.update { it.copy(searching = true, searchError = null) }
            try {
                val results = MoonApi.searchVideo(q.trim())
                _ui.update { it.copy(searching = false, searchResults = results) }
            } catch (e: Exception) {
                _ui.update { it.copy(searching = false, searchError = e.message ?: "搜索失败") }
            }
        }
    }

    fun clearSearch() {
        _ui.update { it.copy(searching = false, searchResults = emptyList(), searchError = null) }
    }

    fun addSearchHistory(kw: String) {
        if (kw.isBlank()) return
        if (_ui.value.searchHistory.contains(kw)) {
            _ui.update { it.copy(searchHistory = listOf(kw) + it.searchHistory.filterNot { h -> h == kw }) }
        } else {
            _ui.update { it.copy(searchHistory = listOf(kw) + it.searchHistory.take(19)) }
        }
        viewModelScope.launch {
            try {
                MoonApi.addSearchHistory(kw)
            } catch (_: Exception) {
            }
        }
    }

    fun deleteSearchHistory(kw: String) {
        _ui.update { it.copy(searchHistory = it.searchHistory.filterNot { h -> h == kw }) }
        viewModelScope.launch {
            try {
                MoonApi.deleteSearchHistory(kw)
            } catch (_: Exception) {
            }
        }
    }

    // ---------------- 详情 ----------------
    fun loadDetail(itemId: String, source: String, onDone: (List<VideoGroup>?, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _ui.update { it.copy(detailLoading = true, detailError = null) }
            try {
                val detail = MoonApi.detail(itemId, source)
                val groups = VideoGroup.group(listOf(detail))
                _ui.update { it.copy(detailLoading = false, detailGroups = groups) }
                onDone(groups, null)
            } catch (e: ApiException) {
                _ui.update { it.copy(detailLoading = false, detailError = friendlyError(e)) }
                onDone(null, friendlyError(e))
            } catch (e: Exception) {
                _ui.update { it.copy(detailLoading = false, detailError = e.message ?: "加载详情失败") }
                onDone(null, e.message ?: "加载详情失败")
            }
        }
    }

    fun clearDetail() {
        _ui.update { it.copy(detailLoading = false, detailError = null, detailGroups = emptyList()) }
    }

    // ---------------- 发现 ----------------
    fun loadDiscover(kind: String = "movie", category: String = "", type: String = "") {
        viewModelScope.launch {
            _ui.update { it.copy(discoverLoading = true, discoverError = null) }
            try {
                val items = MoonApi.getDoubanCategories(kind, category, type, limit = 30)
                _ui.update { it.copy(discoverLoading = false, discoverItems = items) }
            } catch (e: Exception) {
                _ui.update { it.copy(discoverLoading = false, discoverError = e.message ?: "加载失败") }
            }
        }
    }

    // ---------------- 管理 ----------------
    fun loadAdminConfig() {
        viewModelScope.launch {
            _ui.update { it.copy(adminLoading = true, adminError = null) }
            try {
                val cfg = MoonApi.getAdminConfig()
                _ui.update { it.copy(adminLoading = false, adminConfig = cfg.toMap()) }
            } catch (e: ApiException) {
                _ui.update { it.copy(adminLoading = false, adminError = friendlyError(e)) }
            } catch (e: Exception) {
                _ui.update { it.copy(adminLoading = false, adminError = e.message ?: "加载失败") }
            }
        }
    }

    fun adminAction(action: () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                setToast("操作成功")
                loadAdminConfig()
            } catch (e: ApiException) {
                setToast(friendlyError(e))
            } catch (e: Exception) {
                setToast(e.message ?: "操作失败")
            }
        }
    }

    // ---------------- 工具 ----------------
    private fun friendlyError(e: ApiException): String {
        return when {
            e.code == 401 -> "未登录或登录已过期"
            e.code == 403 -> "没有权限"
            e.code >= 500 -> "服务器开小差了(${e.code})"
            else -> e.body.ifBlank { "请求失败(${e.code})" }
        }
    }
}
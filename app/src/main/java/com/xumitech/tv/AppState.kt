package com.xumitech.tv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xumitech.tv.data.AuthStore
import com.xumitech.tv.data.MoonTvApi
import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.model.Favorite
import com.xumitech.tv.model.Following
import com.xumitech.tv.model.PlayRecord
import com.xumitech.tv.model.SearchResult
import com.xumitech.tv.model.ServerConfig
import com.xumitech.tv.model.Source
import com.xumitech.tv.model.TodayUpdatedRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AuthState {
    data object Unknown : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val username: String, val isAdmin: Boolean) : AuthState
}

/** 首页区块：标题 + 条目（按加载顺序渐进上屏）。 */
data class HomeSection(val title: String, val items: List<DoubanItem>)

class AppState(private val store: AuthStore) : ViewModel() {
    var authState by mutableStateOf<AuthState>(AuthState.Unknown)
        private set

    /** 主题模式: "system" / "dark" / "light" */
    var themeMode by mutableStateOf(store.themeMode)
        private set

    var siteName by mutableStateOf("须弥Media")
        private set

    var sources by mutableStateOf<List<Source>>(emptyList())
        private set

    // 首页数据（渐进式：热门先显示，分类/今日陆续追加）
    var homeSections by mutableStateOf<List<HomeSection>>(emptyList())
        private set
    var homeLoading by mutableStateOf(false)
        private set

    // 今日新更
    var todayUpdated by mutableStateOf<TodayUpdatedRecord?>(null)
        private set

    // 收藏 / 播放记录 / 追更 / 搜索历史
    var favorites by mutableStateOf<Map<String, Favorite>>(emptyMap())
        private set
    var playRecords by mutableStateOf<Map<String, PlayRecord>>(emptyMap())
        private set
    var followings by mutableStateOf<Map<String, Following>>(emptyMap())
        private set
    var searchHistory by mutableStateOf<List<String>>(emptyList())
        private set

    /**
     * 播放记录按保存时间倒序（首页「继续观看」用）。
     * 学 Flutter 版：只展示真看过一段的（playTime>5 且 totalTime>0）。
     */
    val recentRecords: List<PlayRecord>
        get() = playRecords.values
            .filter { it.playTime > 5 && it.totalTime > 0 }
            .sortedByDescending { it.saveTime }
            .take(10)

    /** 追更按保存时间倒序（首页「我的追更」用）。 */
    val recentFollowings: List<Pair<String, Following>>
        get() = followings.entries
            .sortedByDescending { it.value.saveTime }
            .take(12)
            .map { it.key to it.value }

    val isLoggedIn: Boolean get() = authState is AuthState.LoggedIn

    init {
        store.restore()
        authState = if (MoonTvApi.isLoggedIn) {
            AuthState.LoggedIn(store.username, MoonTvApi.isAdmin)
        } else {
            AuthState.LoggedOut
        }
        MoonTvApi.onAuthChanged = { refreshAuthFromApi() }
        if (isLoggedIn) refreshBase()
    }

    private fun refreshAuthFromApi() {
        authState = if (MoonTvApi.isLoggedIn) {
            val r = MoonTvApi.parseRole()
            if (r.isNotEmpty()) store.role = r
            AuthState.LoggedIn(store.username, MoonTvApi.isAdmin)
        } else {
            AuthState.LoggedOut
        }
    }

    fun login(server: String, username: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                MoonTvApi.configure(server, null)
                val cookie = MoonTvApi.login(username, password)
                if (cookie.isEmpty()) {
                    onResult("登录失败：未获取到会话")
                    return@launch
                }
                store.saveAuth(cookie, username)
                refreshAuthFromApi()
                refreshBase()
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "登录失败，请检查服务器地址")
            }
        }
    }

    fun register(server: String, username: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                MoonTvApi.configure(server, null)
                val cookie = MoonTvApi.register(username, password)
                if (cookie.isEmpty()) {
                    onResult("注册失败：未获取到会话")
                    return@launch
                }
                store.saveAuth(cookie, username)
                refreshAuthFromApi()
                refreshBase()
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "注册失败，请检查服务器地址")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            MoonTvApi.logout()
            store.clear()
            refreshAuthFromApi()
        }
    }

    /** 切换主题模式: "system" / "dark" / "light" */
    fun setThemeMode(mode: String) {
        themeMode = mode
        store.themeMode = mode
    }

    private fun refreshBase() {
        viewModelScope.launch {
            try {
                val cfg = MoonTvApi.getServerConfig()
                if (cfg.siteName.isNotEmpty()) siteName = cfg.siteName
            } catch (_: Exception) {}
            try {
                sources = MoonTvApi.getSources()
            } catch (_: Exception) {}
        }
    }

    /** 加载首页：热门电影/剧集 → 各分类，每个区块就绪即上屏（渐进式）。 */
    fun loadHome(onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            homeLoading = true
            val sections = mutableListOf<HomeSection>()

            // 并行：今天新更（独立协程，不阻塞首页区块）
            viewModelScope.launch {
                try {
                    todayUpdated = withContext(Dispatchers.IO) {
                        MoonTvApi.getTodayUpdated()
                    }
                } catch (_: Exception) {}
            }

            // 第一梯队：热门（并行）
            val hotJobs = listOf(
                "movie" to "热门电影",
                "tv" to "热门剧集",
            )
            for ((kind, label) in hotJobs) {
                try {
                    val items = withContext(Dispatchers.IO) {
                        MoonTvApi.getDoubanRecommends(kind, limit = 20)
                    }
                    if (items.isNotEmpty()) {
                        sections += HomeSection(label, items)
                        homeSections = sections.toList()
                    }
                } catch (_: Exception) {}
            }

            // 第二梯队：分类（逐类拉取，就绪即追加）
            val cats = listOf(
                Triple("华语", "movie", "热门" to "华语"),
                Triple("欧美", "movie", "热门" to "欧美"),
                Triple("韩国", "movie", "热门" to "韩国"),
                Triple("日本", "movie", "热门" to "日本"),
                Triple("国产剧", "tv", "tv" to "tv_domestic"),
                Triple("美剧", "tv", "tv" to "tv_american"),
                Triple("韩剧", "tv", "tv" to "tv_korean"),
                Triple("日剧", "tv", "tv" to "tv_japanese"),
                Triple("动漫", "tv", "tv" to "tv_animation"),
                Triple("综艺", "tv", "show" to "show_domestic"),
                Triple("纪录片", "tv", "tv" to "tv_documentary"),
            )
            for (c in cats) {
                try {
                    val items = withContext(Dispatchers.IO) {
                        MoonTvApi.getDoubanCategories(
                            kind = c.second,
                            category = c.third.first,
                            type = c.third.second,
                        )
                    }
                    if (items.isNotEmpty()) {
                        sections += HomeSection(c.first, items)
                        homeSections = sections.toList()
                    }
                } catch (_: Exception) {}
            }
            homeLoading = false
            onDone?.invoke()
        }
    }

    /** 聚合详情：按标题搜索所有源，去重分组后返回。 */
    fun searchAggregate(
        query: String,
        onResult: (List<SearchResult>?, String?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val r = MoonTvApi.searchVideo(query)
                onResult(r, null)
            } catch (e: Exception) {
                onResult(null, e.message ?: "搜索失败")
            }
        }
    }

    fun search(query: String, onResult: (List<SearchResult>?, String?) -> Unit) {
        searchAggregate(query, onResult)
    }

    fun detail(id: String, source: String, onResult: (SearchResult?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(MoonTvApi.detail(id, source), null)
            } catch (e: Exception) {
                onResult(null, e.message ?: "加载失败，请稍后重试")
            }
        }
    }

    // ---------------- 收藏 / 播放记录 / 追更 / 搜索历史 ----------------
    fun loadAll() {
        viewModelScope.launch {
            launch { loadFavorites() }
            launch { loadPlayRecords() }
            launch { loadFollowings() }
            launch { loadSearchHistory() }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                favorites = MoonTvApi.getFavorites()
            } catch (_: Exception) {}
        }
    }

    fun toggleFavorite(key: String, f: Favorite) {
        viewModelScope.launch {
            val current = favorites
            if (current.containsKey(key)) {
                try {
                    MoonTvApi.deleteFavorite(key)
                    favorites = current - key
                } catch (_: Exception) {}
            } else {
                try {
                    MoonTvApi.saveFavorite(key, f)
                    favorites = current + (key to f)
                } catch (_: Exception) {}
            }
        }
    }

    fun loadPlayRecords() {
        viewModelScope.launch {
            try {
                playRecords = MoonTvApi.getPlayRecords()
            } catch (_: Exception) {}
        }
    }

    fun saveRecord(key: String, r: PlayRecord) {
        playRecords = playRecords + (key to r)
        viewModelScope.launch {
            try {
                MoonTvApi.savePlayRecord(key, r)
            } catch (_: Exception) {}
        }
    }

    fun removeRecord(key: String) {
        playRecords = playRecords - key
        viewModelScope.launch {
            try {
                MoonTvApi.deletePlayRecord(key)
            } catch (_: Exception) {}
        }
    }

    fun loadFollowings() {
        viewModelScope.launch {
            try {
                followings = MoonTvApi.getFollowings()
            } catch (_: Exception) {}
        }
    }

    fun toggleFollowing(key: String, f: Following) {
        viewModelScope.launch {
            val current = followings
            if (current.containsKey(key)) {
                try {
                    MoonTvApi.deleteFollowing(key)
                    followings = current - key
                } catch (_: Exception) {}
            } else {
                try {
                    MoonTvApi.saveFollowing(key, f)
                    followings = current + (key to f)
                } catch (_: Exception) {}
            }
        }
    }

    fun loadSearchHistory() {
        viewModelScope.launch {
            try {
                searchHistory = MoonTvApi.getSearchHistory()
            } catch (_: Exception) {}
        }
    }

    fun addSearchHistory(keyword: String) {
        viewModelScope.launch {
            try {
                searchHistory = MoonTvApi.addSearchHistory(keyword)
            } catch (_: Exception) {}
        }
    }

    fun deleteSearchHistory(keyword: String) {
        viewModelScope.launch {
            try {
                MoonTvApi.deleteSearchHistory(keyword)
                // 本地同步移除
                searchHistory = searchHistory - keyword
            } catch (_: Exception) {}
        }
    }

    /** 修改密码。失败抛带后端错误信息的异常。 */
    fun changePassword(oldPassword: String, newPassword: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                MoonTvApi.changePassword(oldPassword, newPassword)
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "修改失败")
            }
        }
    }
}
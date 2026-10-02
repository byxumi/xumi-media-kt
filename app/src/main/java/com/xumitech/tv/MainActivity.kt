package com.xumitech.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xumitech.tv.data.AuthStore
import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.ui.components.LiquidNavBar
import com.xumitech.tv.ui.components.NavTab
import com.xumitech.tv.ui.screens.DetailScreen
import com.xumitech.tv.ui.screens.FavoritesScreen
import com.xumitech.tv.ui.screens.HistoryScreen
import com.xumitech.tv.ui.screens.HomeScreen
import com.xumitech.tv.ui.screens.LoginScreen
import com.xumitech.tv.ui.screens.ProfileScreen
import com.xumitech.tv.ui.screens.SearchScreen
import com.xumitech.tv.ui.theme.XumiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState: AppState = viewModel(
                factory = AppStateFactory(applicationContext),
            )
            // 主题跟随 AppState.themeMode（"system" 随系统 / "dark" 强制深色 / "light" 强制浅色）
            val darkTheme = when (appState.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            XumiTheme(darkTheme = darkTheme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var showLogin by remember {
                        mutableStateOf(
                            appState.authState is AuthState.Unknown ||
                                appState.authState is AuthState.LoggedOut,
                        )
                    }
                    if (showLogin) {
                        LoginScreen(
                            appState = appState,
                            onLoggedIn = { showLogin = false },
                        )
                    } else {
                        MainScaffold(
                            appState = appState,
                            onLogout = {
                                appState.logout()
                                showLogin = true
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(
    appState: AppState,
    onLogout: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var openItem by remember { mutableStateOf<DoubanItem?>(null) }
    // 历史续播：携带初始集 + 断点秒数
    var resumeRequest by remember { mutableStateOf<Triple<DoubanItem, Int, Long>?>(null) }
    var discoverOpen by rememberSaveable { mutableStateOf(false) }
    var adminOpen by rememberSaveable { mutableStateOf(false) }

    // 覆盖层互斥：进入任一全屏页时关闭其他覆盖层，避免多层叠加的交互混乱
    fun openDetail(item: DoubanItem, episode: Int = 0, startSec: Long = 0L) {
        searchOpen = false
        discoverOpen = false
        adminOpen = false
        if (episode > 0 || startSec > 0L) resumeRequest = Triple(item, episode, startSec)
        else openItem = item
    }
    fun closeDetail() {
        openItem = null
        resumeRequest = null
    }

    val tabs = listOf(
        NavTab("首页", Icons.Rounded.Home, Icons.Rounded.Home),
        NavTab("收藏", Icons.Rounded.FavoriteBorder, Icons.Rounded.Favorite),
        NavTab("历史", Icons.Rounded.History, Icons.Rounded.History),
        NavTab("我的", Icons.Rounded.PersonOutline, Icons.Rounded.Person),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            LiquidNavBar(
                tabs = tabs,
                selectedIndex = tab,
                onSelected = { tab = it },
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (tab) {
                0 -> HomeScreen(
                    appState = appState,
                    onSearch = { searchOpen = true },
                    onOpenItem = { item -> openDetail(item) },
                    onOpenDiscover = {
                        searchOpen = false
                        discoverOpen = true
                    },
                )
                1 -> FavoritesScreen(appState, onOpenItem = { item -> openDetail(item) })
                2 -> HistoryScreen(
                    appState,
                    onOpenItem = { item -> openDetail(item) },
                    onResume = { item, ep, sec ->
                        openDetail(item, ep, sec)
                    },
                )
                3 -> ProfileScreen(
                    appState,
                    onLogout,
                    onOpenAdmin = { adminOpen = true },
                )
            }
        }
    }

    // 搜索页（全屏覆盖）
    if (searchOpen) {
        SearchScreen(
            appState = appState,
            onClose = { searchOpen = false },
            onOpenItem = {
                openDetail(it)
            },
        )
    }

    // 分类榜单页
    if (discoverOpen) {
        com.xumitech.tv.ui.screens.DiscoverScreen(
            appState = appState,
            onClose = { discoverOpen = false },
            onOpenItem = {
                openDetail(it)
            },
        )
    }

    // 管理后台（全屏覆盖，仅管理员）
    if (adminOpen) {
        com.xumitech.tv.ui.screens.AdminScreen(
            onClose = { adminOpen = false },
        )
    }

    // 详情页（全屏覆盖；历史续播优先）
    val detailReq = resumeRequest ?: openItem?.let { Triple(it, 0, 0L) }
    detailReq?.let { (item, ep, sec) ->
        DetailScreen(
            appState = appState,
            item = item,
            onClose = ::closeDetail,
            initialEpisode = ep,
            startPositionSec = sec,
        )
    }
}
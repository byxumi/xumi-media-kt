package com.xumitech.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xumitech.tv.data.VideoGroup
import com.xumitech.tv.ui.screens.AdminScreen
import com.xumitech.tv.ui.screens.DetailScreen
import com.xumitech.tv.ui.screens.DiscoverScreen
import com.xumitech.tv.ui.screens.FavoritesScreen
import com.xumitech.tv.ui.screens.HistoryScreen
import com.xumitech.tv.ui.screens.HomeScreen
import com.xumitech.tv.ui.screens.LoginScreen
import com.xumitech.tv.ui.screens.PlayScreen
import com.xumitech.tv.ui.screens.ProfileScreen
import com.xumitech.tv.ui.screens.SearchScreen
import com.xumitech.tv.ui.state.AppUiState
import com.xumitech.tv.ui.state.AppViewModel
import com.xumitech.tv.ui.state.AuthState
import com.xumitech.tv.ui.theme.XumiTheme

/** 覆盖层目标。 */
sealed interface Overlay {
    data object None : Overlay
    data object Search : Overlay
    data object Discover : Overlay
    data object Admin : Overlay
    data class Detail(val target: DetailTarget) : Overlay
    data class Play(val req: PlayRequest) : Overlay
}

/** 详情页轻量入口(从任意卡片点击而来)。 */
data class DetailTarget(
    val id: String,
    val source: String,
    val title: String,
    val poster: String,
    val year: String,
)

/** 播放请求:分组 + 源/集索引 + 续播秒。 */
data class PlayRequest(
    val group: VideoGroup,
    val sourceIndex: Int,
    val episodeIndex: Int,
    val startSec: Long,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val ui by vm.ui.collectAsState()
            val dark = when (ui.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            XumiTheme(darkTheme = dark) {
                AppRoot(vm, ui)
            }
        }
    }
}

@Composable
private fun AppRoot(vm: AppViewModel, ui: AppUiState) {
    when (ui.auth) {
        AuthState.Unknown -> { /* 启动瞬间,空帧 */ }
        AuthState.LoggedOut -> LoginScreen(vm)
        is AuthState.LoggedIn -> LoggedInRoot(vm, ui)
    }
}

@Composable
private fun LoggedInRoot(vm: AppViewModel, ui: AppUiState) {
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    fun openDetail(target: DetailTarget) {
        overlay = Overlay.Detail(target)
    }

    fun openPlay(req: PlayRequest) {
        overlay = Overlay.Play(req)
    }

    val closeOverlay: () -> Unit = { overlay = Overlay.None }

    Box {
        MainScaffold(
            vm = vm,
            ui = ui,
            tab = tab,
            onTabSelected = { tab = it },
            onOpenDetail = ::openDetail,
            onOpenSearch = { overlay = Overlay.Search },
            onOpenDiscover = { overlay = Overlay.Discover },
            onOpenAdmin = { overlay = Overlay.Admin },
        )
        when (val o = overlay) {
            Overlay.None -> {}
            Overlay.Search -> SearchScreen(vm, ui, onClose = closeOverlay, onOpenDetail = ::openDetail)
            Overlay.Discover -> DiscoverScreen(vm, ui, onClose = closeOverlay, onOpenDetail = ::openDetail)
            Overlay.Admin -> AdminScreen(onClose = closeOverlay)
            is Overlay.Detail -> DetailScreen(
                vm, ui,
                target = o.target,
                onClose = closeOverlay,
                onOpenPlay = { group, srcIdx, epIdx, startSec ->
                    openPlay(PlayRequest(group, srcIdx, epIdx, startSec))
                },
            )
            is Overlay.Play -> PlayScreen(
                vm, ui,
                request = o.req,
                onClose = closeOverlay,
                onOpenDetail = { target, srcIdx ->
                    overlay = Overlay.Detail(target)
                },
            )
        }
    }
}

/** 底部四 Tab 主框架。 */
@Composable
private fun MainScaffold(
    vm: AppViewModel,
    ui: AppUiState,
    tab: Int,
    onTabSelected: (Int) -> Unit,
    onOpenDetail: (DetailTarget) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenAdmin: () -> Unit,
) {
    val tabs = listOf<Pair<String, ImageVector>>(
        "首页" to Icons.Filled.Home,
        "收藏" to Icons.Filled.FavoriteBorder,
        "历史" to Icons.Filled.History,
        "我的" to Icons.Filled.PersonOutline,
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                tabs.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { onTabSelected(i) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            0 -> HomeScreen(
                vm, ui,
                modifier = Modifier.padding(padding),
                onOpenDetail = onOpenDetail,
                onOpenSearch = onOpenSearch,
                onOpenDiscover = onOpenDiscover,
            )
            1 -> FavoritesScreen(vm, ui, modifier = Modifier.padding(padding), onOpenDetail = onOpenDetail)
            2 -> HistoryScreen(vm, ui, modifier = Modifier.padding(padding), onOpenDetail = onOpenDetail)
            3 -> ProfileScreen(vm, ui, modifier = Modifier.padding(padding), onOpenAdmin = onOpenAdmin)
        }
    }
}

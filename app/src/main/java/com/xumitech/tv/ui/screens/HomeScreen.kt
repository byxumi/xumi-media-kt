package com.xumitech.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.xumitech.tv.AppState
import com.xumitech.tv.HomeSection
import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.model.PlayRecord
import com.xumitech.tv.ui.components.ContinueWatchingCard
import com.xumitech.tv.ui.components.EmptyState
import com.xumitech.tv.ui.components.FollowingCard
import com.xumitech.tv.ui.components.GradientProgressBar
import com.xumitech.tv.ui.components.HomeSkeleton
import com.xumitech.tv.ui.components.PosterTile
import com.xumitech.tv.ui.components.SectionHeader
import com.xumitech.tv.ui.components.ShimmerBox
import com.xumitech.tv.ui.components.TodayUpdatedCard
import com.xumitech.tv.ui.theme.ScrimHeavy
import com.xumitech.tv.ui.theme.ScrimHeroBottom
import com.xumitech.tv.ui.theme.ScrimHeroTop
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appState: AppState,
    onSearch: () -> Unit,
    onOpenItem: (DoubanItem) -> Unit,
    onOpenDiscover: () -> Unit,
) {
    val sections = appState.homeSections
    val loading = appState.homeLoading
    // 顶层的状态读取会被 Compose 追踪：这些数据变化会触发重组
    val records = appState.recentRecords
    val followings = appState.recentFollowings
    val today = appState.todayUpdated
    val todayItems = today?.items?.take(12) ?: emptyList()
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Hero：取第一个 section 的前 3 部（横滑大卡轮播）
    val heroItems = sections.firstOrNull()?.items?.take(3) ?: emptyList()
    val restSections = if (heroItems.isNotEmpty()) {
        sections.mapIndexed { i, s ->
            if (i == 0) s.copy(items = s.items.drop(3)) else s
        }.filter { it.items.isNotEmpty() }
    } else sections

    fun refreshHome() {
        scope.launch {
            refreshing = true
            appState.loadHome {
                appState.loadAll()
                refreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        appState.loadHome()
        // 收藏/记录/追更/搜索历史：已有内存缓存则跳过，避免切 tab 重复请求
        if (appState.favorites.isEmpty()) appState.loadFavorites()
        if (appState.playRecords.isEmpty()) appState.loadPlayRecords()
        if (appState.followings.isEmpty()) appState.loadFollowings()
        if (appState.searchHistory.isEmpty()) appState.loadSearchHistory()
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = ::refreshHome,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
        item {
            HomeHeader(appState, onSearch, onOpenDiscover)
        }

        // Hero 大卡（布局族 1：16:9 横滑大卡）
        if (heroItems.isNotEmpty()) {
            item {
                HeroRow(heroItems, onOpenItem)
            }
        }

        // 继续观看（布局族 2：列表行 + 进度条）
        if (records.isNotEmpty()) {
            item {
                SectionHeader("继续观看")
            }
            item {
                Column {
                    records.forEach { r ->
                        ContinueRow(
                            record = r,
                            onClick = {
                                onOpenItem(
                                    DoubanItem(
                                        id = "",
                                        title = r.title,
                                        poster = r.cover,
                                        source = "",
                                        sourceName = r.sourceName,
                                        year = r.year,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }

        // 我的追更（布局族 3：2 列网格）
        if (followings.isNotEmpty()) {
            item {
                SectionHeader("我的追更")
            }
            item {
                // LazyVerticalGrid 不能嵌套在 LazyColumn item 内（无限高度会崩溃），
                // 这里按行手动切分，每行 2 个
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    followings.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { (key, f) ->
                                FollowingCard(
                                    title = f.title,
                                    poster = f.cover,
                                    watchedEpisodes = f.watchedEpisodes,
                                    totalEpisodes = f.totalEpisodes,
                                    onClick = {
                                        onOpenItem(
                                            DoubanItem(
                                                id = "",
                                                title = f.title,
                                                poster = f.cover,
                                                source = "",
                                                sourceName = f.sourceName,
                                                year = f.year,
                                            ),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 今日新更（布局族 4：2 列网格）
        if (todayItems.isNotEmpty()) {
            item {
                SectionHeader("今日新更", subtitle = today?.date?.takeIf { it.isNotBlank() })
            }
            item {
                // LazyVerticalGrid 不能嵌套在 LazyColumn item 内，同样按行切分
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    todayItems.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { it ->
                                TodayUpdatedCard(
                                    title = it.title,
                                    poster = it.poster,
                                    sourceName = it.sourceName,
                                    newEpisodes = it.newEpisodes,
                                    onClick = {
                                        onOpenItem(
                                            DoubanItem(
                                                id = "",
                                                title = it.title,
                                                poster = it.poster,
                                                source = "",
                                                sourceName = it.sourceName,
                                                year = it.year,
                                            ),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (restSections.isEmpty()) {
            item {
                if (loading) {
                    Column {
                        repeat(3) { HomeSkeleton() }
                    }
                } else {
                    EmptyState(
                        text = "暂无内容，点击重试",
                        onRetry = { appState.loadHome() },
                    )
                }
            }
        } else {
            items(restSections.size) { i ->
                val section = restSections[i]
                SectionRow(section.title, section.items, onOpenItem)
            }
        }
        }
    }
}

/** Hero 大卡：16:9 大图 + 渐变遮罩 + 标题 + 播放按钮。 */
@Composable
private fun HeroRow(
    items: List<DoubanItem>,
    onOpenItem: (DoubanItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id + it.source }) { item ->
            Box(
                Modifier
                    .width(320.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenItem(item) },
            ) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(ScrimHeroTop, ScrimHeroBottom),
                            ),
                        ),
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                ) {
                    Text(
                        item.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.22f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "立即播放",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            item.year,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

/** 继续观看列表行（布局族 2：横向封面 + 文本 + 进度条）。 */
@Composable
private fun ContinueRow(
    record: PlayRecord,
    onClick: () -> Unit,
) {
    val progress = if (record.totalTime > 0) {
        (record.playTime.toFloat() / record.totalTime).coerceIn(0f, 1f)
    } else 0f
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(96.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            AsyncImage(
                model = record.cover,
                contentDescription = record.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(ScrimHeavy.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                record.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (record.totalEpisodes > 1) {
                    "${record.sourceName} · 第${record.index}集 / 共${record.totalEpisodes}集"
                } else {
                    record.sourceName
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            GradientProgressBar(progress, height = 3.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${(record.playTime / 60)}分",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HomeHeader(
    appState: AppState,
    onSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                appState.siteName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            val uname = (appState.authState as? com.xumitech.tv.AuthState.LoggedIn)?.username ?: ""
            Text(
                "欢迎，$uname",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            // 分类榜单入口
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onOpenDiscover() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "分类·榜单",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // 搜索框（毛玻璃感卡片）
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                )
                .clickable { onSearch() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 16.dp).size(22.dp),
            )
            Text(
                "聚合搜索影视、剧集…",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

@Composable
private fun SectionRow(
    title: String,
    items: List<DoubanItem>,
    onOpenItem: (DoubanItem) -> Unit,
) {
    Column(Modifier.padding(top = 16.dp)) {
        SectionHeader(title = title, subtitle = if (items.size > 10) "${items.size} 部" else null)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id + it.source }) { item ->
                PosterTile(item, onClick = { onOpenItem(item) })
            }
        }
    }
}

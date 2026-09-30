package com.xumitech.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xumitech.tv.AppState
import com.xumitech.tv.HomeSection
import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.ui.components.ContinueWatchingCard
import com.xumitech.tv.ui.components.EmptyState
import com.xumitech.tv.ui.components.HomeSkeleton
import com.xumitech.tv.ui.components.PosterTile
import com.xumitech.tv.ui.components.SectionHeader
import com.xumitech.tv.ui.components.ShimmerBox

@Composable
fun HomeScreen(
    appState: AppState,
    onSearch: () -> Unit,
    onOpenItem: (DoubanItem) -> Unit,
    onOpenDiscover: () -> Unit,
) {
    val sections = appState.homeSections
    val loading = appState.homeLoading

    LaunchedEffect(Unit) {
        appState.loadHome()
        appState.loadAll()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            HomeHeader(appState, onSearch, onOpenDiscover)
        }

        // 继续观看
        val records = appState.recentRecords
        if (records.isNotEmpty()) {
            item {
                SectionHeader("继续观看")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(records, key = { it.title + it.sourceName + it.index }) { r ->
                        ContinueWatchingCard(
                            record = r,
                            onClick = {
                                // 历史续播：扁平化条目，进入详情页自动恢复
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

        if (sections.isEmpty()) {
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
            items(sections.size) { i ->
                val section = sections[i]
                SectionRow(section.title, section.items, onOpenItem)
            }
        }
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
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            val uname = (appState.authState as? com.xumitech.tv.AuthState.LoggedIn)?.username ?: ""
            Text(
                "欢迎，$uname",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
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
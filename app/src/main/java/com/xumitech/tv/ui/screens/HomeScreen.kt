package com.xumitech.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xumitech.tv.DetailTarget
import com.xumitech.tv.data.DoubanItem
import com.xumitech.tv.data.Following
import com.xumitech.tv.data.PlayRecord
import com.xumitech.tv.data.TodayUpdatedItem
import com.xumitech.tv.ui.components.ContinueWatchingCard
import com.xumitech.tv.ui.components.FollowingCard
import com.xumitech.tv.ui.components.HomeSkeleton
import com.xumitech.tv.ui.components.PosterTile
import com.xumitech.tv.ui.components.SectionHeader
import com.xumitech.tv.ui.components.SimpleEmpty
import com.xumitech.tv.ui.components.SimpleError
import com.xumitech.tv.ui.components.TodayUpdatedCard
import com.xumitech.tv.ui.state.AppUiState
import com.xumitech.tv.ui.state.AppViewModel

/** 首页:顶部操作行 + 继续观看 + 追更 + 今日更新 + 热门分栏。 */
@Composable
fun HomeScreen(
    vm: AppViewModel,
    ui: AppUiState,
    modifier: Modifier = Modifier,
    onOpenDetail: (DetailTarget) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
) {
    LaunchedEffect(Unit) {
        if (!ui.homeLoading && ui.homeSections.isEmpty() && ui.homeError == null) {
            vm.loadHome()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    ui.siteName.ifBlank { "须弥" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    "今晚看什么？",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Rounded.Search, "搜索", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        when {
            ui.homeLoading && ui.homeSections.isEmpty() -> HomeSkeleton(modifier = Modifier.fillMaxSize())
            ui.homeError != null && ui.homeSections.isEmpty() -> SimpleError(
                text = ui.homeError,
                onRetry = { vm.loadHome() },
                modifier = Modifier.fillMaxSize(),
            )
            ui.homeSections.isEmpty() && ui.todayUpdated == null && ui.playRecords.isEmpty() && ui.followings.isEmpty() ->
                SimpleEmpty(
                    text = "还没有内容",
                    subtitle = "去发现页逛逛,聚合全网影视源",
                    actionLabel = "去发现",
                    onAction = onOpenDiscover,
                    modifier = Modifier.fillMaxSize(),
                )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                // 继续观看
                if (ui.recentRecords.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "继续观看",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            action = { /* 无 */ },
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(ui.recentRecords, key = { it.title + it.sourceName }) { r: PlayRecord ->
                                ContinueWatchingCard(
                                    record = r,
                                    onClick = { onOpenDetail(recordTarget(r)) },
                                )
                            }
                        }
                    }
                }

                // 我的追更
                if (ui.followings.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "我的追更",
                            subtitle = "${ui.followings.size} 部",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                    item {
                        val rows = ui.followings.values.sortedByDescending { it.saveTime }.chunked(2)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rows.forEachIndexed { _, pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    pair.forEach { f: Following ->
                                        FollowingCard(
                                            following = f,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onOpenDetail(followingTarget(f)) },
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // 今日更新
                ui.todayUpdated?.items?.takeIf { it.isNotEmpty() }?.let { today ->
                    item {
                        SectionHeader(
                            title = "今日更新",
                            subtitle = ui.todayUpdated.date,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(today, key = { it.source + it.id }) { t: TodayUpdatedItem ->
                                TodayUpdatedCard(item = t, onClick = { onOpenDetail(todayTarget(t)) })
                            }
                        }
                    }
                }

                // 热门分栏
                ui.homeSections.forEach { section ->
                    item {
                        SectionHeader(
                            title = section.title,
                            subtitle = "热门推荐",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            action = {
                                androidx.compose.material3.TextButton(onClick = onOpenDiscover) {
                                    Text("发现")
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowForward,
                                        null,
                                        modifier = Modifier.height(16.dp),
                                    )
                                }
                            },
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(section.items, key = { it.source + it.id }) { d: DoubanItem ->
                                PosterTile(
                                    title = d.title,
                                    poster = d.poster,
                                    width = 112.dp,
                                    rate = d.rate.takeIf { it.isNotBlank() },
                                    onClick = { onOpenDetail(doubanTarget(d)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun doubanTarget(d: DoubanItem) = DetailTarget(id = d.id, source = d.source, title = d.title, poster = d.poster, year = d.year)

private fun recordTarget(r: PlayRecord) = DetailTarget(
    id = r.id.takeIf { it.isNotBlank() } ?: r.title,
    source = r.source.takeIf { it.isNotBlank() } ?: r.sourceName,
    title = r.title,
    poster = r.cover,
    year = r.year,
)

private fun followingTarget(f: Following) = DetailTarget(
    id = f.id.takeIf { it.isNotBlank() } ?: f.title,
    source = f.source.takeIf { it.isNotBlank() } ?: f.sourceName,
    title = f.title,
    poster = f.cover,
    year = f.year,
)

private fun todayTarget(t: TodayUpdatedItem) = DetailTarget(id = t.id, source = t.source, title = t.title, poster = t.poster, year = t.year)

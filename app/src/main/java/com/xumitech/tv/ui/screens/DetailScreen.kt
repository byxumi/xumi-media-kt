package com.xumitech.tv.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.model.Favorite
import com.xumitech.tv.model.Following
import com.xumitech.tv.model.SearchResult
import com.xumitech.tv.model.VideoGroup
import com.xumitech.tv.ui.components.CenterLoading
import com.xumitech.tv.ui.components.EmptyState
import com.xumitech.tv.ui.components.GradientProgressBar
import com.xumitech.tv.ui.theme.ScrimHeavy
import com.xumitech.tv.ui.theme.ScrimHeroBottom
import com.xumitech.tv.ui.theme.ScrimHeroTop

/**
 * 详情页（播放前页面）：
 * 1. 通过「标题聚合」拉取该影片全部源（VideoGroup.group 去重）
 * 2. 头部立即渲染（标题/年份/类型 + 海报）
 * 3. 播放按钮 + 收藏/追更 + 进度条
 * 4. 简介（可展开）、源列表、选集（6 列网格）
 * 5. 自动选中历史记录所在源（继续播放）
 */
@Composable
fun DetailScreen(
    appState: AppState,
    item: DoubanItem,
    onClose: () -> Unit,
    initialEpisode: Int = 0,
    startPositionSec: Long = 0,
) {
    var group by remember { mutableStateOf<VideoGroup?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var selectedSource by remember { mutableIntStateOf(0) }
    var selectedEpisode by remember { mutableIntStateOf(initialEpisode) }
    var playRequest by remember { mutableStateOf<PlayRequest?>(null) }

    // 加载：标题聚合所有源（reloadKey 变化 = 用户点击重试）
    LaunchedEffect(item.id, item.title, reloadKey) {
        appState.loadFavorites()
        appState.loadFollowings()
        loading = true
        error = null
        appState.searchAggregate(item.title) { results, err ->
            if (err != null) {
                // 兜底：如果 item 带 source/id，直接 detail
                if (item.id.isNotEmpty() && item.source.isNotEmpty()) {
                    appState.detail(item.id, item.source) { r, e2 ->
                        if (r != null) {
                            group = VideoGroup(
                                title = r.title,
                                poster = r.poster,
                                year = r.year,
                                typeName = r.typeName,
                                desc = r.desc,
                                sources = listOf(r),
                            )
                        } else {
                            error = e2 ?: "加载失败"
                        }
                        loading = false
                    }
                } else {
                    error = err
                    loading = false
                }
            } else {
                val g = VideoGroup.group(results ?: emptyList())
                    .firstOrNull { it.title == item.title || VideoGroup.normTitle(it.title) == VideoGroup.normTitle(item.title) }
                    ?: VideoGroup.group(results ?: emptyList()).firstOrNull()
                if (g != null) {
                    group = g
                    // 自动选中历史记录所在源
                    selectResumeSource(appState, g) { idx ->
                        selectedSource = idx
                    }
                } else {
                    error = "未找到「${item.title}」的播放源"
                }
                loading = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            loading -> CenterLoading()
            error != null -> EmptyState(
                error!!,
                icon = Icons.Rounded.SearchOff,
                onRetry = { reloadKey++ },
            )
            group == null -> EmptyState(
                "无可用数据",
                icon = Icons.Rounded.VideoLibrary,
                onRetry = { reloadKey++ },
            )
            else -> {
                val g = group!!
                DetailContent(
                    appState = appState,
                    group = g,
                    item = item,
                    selectedSource = selectedSource,
                    onSourceSelect = { selectedSource = it },
                    selectedEpisode = selectedEpisode,
                    onEpisodeSelect = { selectedEpisode = it },
                    startPositionSec = startPositionSec,
                    onPlay = {
                        val src = g.sources.getOrNull(selectedSource) ?: return@DetailContent
                        playRequest = PlayRequest(
                            source = src,
                            group = g,
                            episode = selectedEpisode,
                            startSec = if (startPositionSec > 0 && selectedEpisode == initialEpisode) {
                                startPositionSec
                            } else 0L,
                        )
                    },
                    onClose = onClose,
                )
            }
        }
    }

    // 播放器（全屏覆盖）
    playRequest?.let { req ->
        PlayScreen(
            appState = appState,
            group = req.group,
            initialSource = req.source,
            initialEpisode = req.episode,
            startPositionSec = req.startSec,
            onClose = { playRequest = null },
        )
    }
}

/** 播放请求（从详情进入播放器）。 */
data class PlayRequest(
    val source: SearchResult,
    val group: VideoGroup,
    val episode: Int,
    val startSec: Long,
)

/**
 * 自动选中历史记录所在源（保存时间最新）。
 */
private fun selectResumeSource(
    appState: AppState,
    group: VideoGroup,
    onDone: (Int) -> Unit,
) {
    val records = appState.playRecords
    if (records.isEmpty()) return
    // 找 title 匹配的最近记录
    val norm = VideoGroup.normTitle(group.title)
    val best = records.values
        .filter { VideoGroup.normTitle(it.title) == norm || it.searchTitle == group.title }
        .maxByOrNull { it.saveTime }
        ?: return
    val idx = group.sources.indexOfFirst { it.source == best.sourceName || it.sourceName == best.sourceName }
    if (idx >= 0) onDone(idx)
}

@Composable
private fun DetailContent(
    appState: AppState,
    group: VideoGroup,
    item: DoubanItem,
    selectedSource: Int,
    onSourceSelect: (Int) -> Unit,
    selectedEpisode: Int,
    onEpisodeSelect: (Int) -> Unit,
    startPositionSec: Long,
    onPlay: () -> Unit,
    onClose: () -> Unit,
) {
    val current = group.sources.getOrNull(selectedSource) ?: group.sources.first()
    val record = appState.playRecords.values
        .firstOrNull { it.sourceName == current.sourceName && it.title == current.title }
    val isFav = appState.favorites.containsKey(current.key)
    val isFollowing = appState.followings.containsKey(current.key)

    // 已看集数（学网页端追更 watched_episodes：取本片播放记录里看过的最大集数，截断到当前源集数）
    val watchedCount = (group.sources.maxOfOrNull { src ->
        appState.playRecords[src.key]?.index ?: 0
    } ?: 0).coerceAtMost(current.episodes.size)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // 封面头图
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                AsyncImage(
                    model = group.poster,
                    contentDescription = group.title,
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
                // 返回键
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .padding(8.dp)
                        .background(
                            ScrimHeavy.copy(alpha = 0.4f),
                            RoundedCornerShape(50),
                        ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                    )
                }
                // 标题
                Text(
                    group.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                )
                // 年份/类型角标（信息分层）
                Row(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (group.typeName.isNotEmpty()) {
                        Text(
                            group.typeName,
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .background(ScrimHeroTop, MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    if (group.year.isNotEmpty()) {
                        Text(
                            group.year,
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .background(ScrimHeroTop, MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }

        // 播放按钮区
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (record != null && record.playTime > 0) {
                            if (record.totalEpisodes > 1) "继续播放 第${record.index}集"
                            else "继续播放"
                        } else {
                            "立即播放"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                // 收藏
                IconButton(
                    onClick = {
                        appState.toggleFavorite(
                            current.key,
                            Favorite(
                                title = current.title,
                                sourceName = current.sourceName,
                                cover = current.poster,
                                year = current.year,
                                saveTime = System.currentTimeMillis(),
                                searchTitle = current.title,
                            ),
                        )
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isFav) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            MaterialTheme.shapes.medium,
                        ),
                ) {
                    Icon(
                        if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isFav) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Spacer(Modifier.width(8.dp))
                // 追更
                IconButton(
                    onClick = {
                        appState.toggleFollowing(
                            current.key,
                            Following(
                                title = current.title,
                                sourceName = current.sourceName,
                                totalEpisodes = current.episodes.size,
                                watchedEpisodes = watchedCount,
                                year = current.year,
                                cover = current.poster,
                                saveTime = System.currentTimeMillis(),
                                searchTitle = current.title,
                            ),
                        )
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isFollowing) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            MaterialTheme.shapes.medium,
                        ),
                ) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = "追更",
                        tint = if (isFollowing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }

        // 继续播放进度条（有记录时）
        if (record != null && record.playTime > 0 && record.totalTime > 0) {
            item {
                val progress = (record.playTime.toFloat() / record.totalTime).coerceIn(0f, 1f)
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GradientProgressBar(
                        progress = progress,
                        modifier = Modifier.weight(1f),
                        height = 5.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "已播放 ${(record.playTime / 60)}分",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        }

        // 简介（可展开）
        if (group.desc.isNotEmpty()) {
            item {
                var descExpanded by remember { mutableStateOf(false) }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "简介",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                    Text(
                        group.desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        lineHeight = 20.sp,
                        maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (group.desc.length > 80 && !descExpanded) {
                        Text(
                            "展开",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { descExpanded = true }
                                .padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        // 播放源
        if (group.sources.size > 1) {
            item {
                Text(
                    "播放源（${group.sources.size}）",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(group.sources.size) { i ->
                        SelectableChip(
                            label = group.sources[i].sourceName,
                            selected = i == selectedSource,
                            onClick = { onSourceSelect(i) },
                        )
                    }
                }
            }
        }

        // 选集（6 列网格）
        if (current.episodes.isNotEmpty()) {
            item {
                Text(
                    "选集（${current.episodes.size}）",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            item {
                EpisodeGrid(
                    labels = current.episodes.mapIndexed { i, _ -> current.episodeTitle(i) },
                    selectedIndex = selectedEpisode,
                    onSelect = onEpisodeSelect,
                )
            }
        }
    }
}

/** 单个可选胶囊（源/选集共用，圆角统一 extraSmall=10dp）。 */
@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 1,
        )
    }
}

/** 选集网格：每行最多 6 个胶囊。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeGrid(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        maxItemsInEachRow = 6,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { i, label ->
            SelectableChip(
                label = label,
                selected = i == selectedIndex,
                onClick = { onSelect(i) },
            )
        }
    }
}

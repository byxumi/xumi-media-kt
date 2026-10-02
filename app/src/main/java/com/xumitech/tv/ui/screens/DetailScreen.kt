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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xumitech.tv.DetailTarget
import com.xumitech.tv.data.SearchResult
import com.xumitech.tv.data.VideoGroup
import com.xumitech.tv.ui.components.SimpleEmpty
import com.xumitech.tv.ui.components.SimpleError
import com.xumitech.tv.ui.state.AppUiState
import com.xumitech.tv.ui.state.AppViewModel
import com.xumitech.tv.ui.theme.Accent
import com.xumitech.tv.ui.theme.ShapeLg
import com.xumitech.tv.ui.theme.ShapeMd

/** 详情页:头图 + 信息层 + 源/选集 + 简介。 */
@Composable
fun DetailScreen(
    vm: AppViewModel,
    ui: AppUiState,
    target: DetailTarget,
    onClose: () -> Unit,
    onOpenPlay: (VideoGroup, Int, Int, Long) -> Unit,
) {
    LaunchedEffect(target.id, target.source) {
        vm.loadDetail(target.id, target.source)
    }

    val group = ui.detailGroups.firstOrNull()
    var sourceIdx by remember(target.id, target.source) { mutableIntStateOf(0) }
    var episodeIdx by remember(target.id, target.source) { mutableIntStateOf(0) }

    when {
        ui.detailLoading -> Box(Modifier.fillMaxSize()) {
            androidx.compose.material3.CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(onClick = onClose, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
            }
        }
        ui.detailError != null && group == null -> Box(Modifier.fillMaxSize()) {
            SimpleError(
                text = ui.detailError,
                detail = "该影片可能已下架或源不可用",
                onRetry = { vm.loadDetail(target.id, target.source) },
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(onClick = onClose, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
            }
        }
        group == null -> Box(Modifier.fillMaxSize()) {
            SimpleEmpty(text = "暂无可用数据", modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onClose, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
            }
        }
        else -> DetailContent(
            vm = vm,
            ui = ui,
            group = group,
            sourceIdx = sourceIdx,
            episodeIdx = episodeIdx,
            onSourceSelect = { sourceIdx = it; episodeIdx = 0 },
            onEpisodeSelect = { episodeIdx = it },
            onPlay = { s, e, st -> onOpenPlay(group, s, e, st) },
            onClose = onClose,
        )
    }
}

@Composable
private fun DetailContent(
    vm: AppViewModel,
    ui: AppUiState,
    group: VideoGroup,
    sourceIdx: Int,
    episodeIdx: Int,
    onSourceSelect: (Int) -> Unit,
    onEpisodeSelect: (Int) -> Unit,
    onPlay: (Int, Int, Long) -> Unit,
    onClose: () -> Unit,
) {
    val sources = group.sources
    val current = sources.getOrNull(sourceIdx) ?: sources.firstOrNull()
    val episodes = current?.episodes ?: emptyList()
    val episodesTitles = current?.episodesTitles ?: emptyList()
    val isFav = current != null && vm.isFavorite(current)
    val isFollowing = current != null && vm.isFollowing(current)
    // 续播:当前源的播放记录(选择集/秒数)
    val resumeRecord = current?.let { ui.playRecords[it.key] }
    val resumeStartSec = resumeRecord?.let { if (it.totalTime > 0) it.playTime.toLong() else 0L } ?: 0L

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // 头图
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
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
                                listOf(Color(0x55000000), Color(0xE60B0C12)),
                            ),
                        ),
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .padding(12.dp)
                        .background(Color(0x40000000), RoundedCornerShape(50)),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = Color.White)
                }
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                ) {
                    Text(
                        group.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (group.year.isNotBlank() || group.typeName.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            listOf(group.year, group.typeName).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        // 操作按钮
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        if (current != null) {
                            val rec = ui.playRecords[current.key]
                            val ep = rec?.takeIf { it.index > 0 && it.index <= episodes.size }?.index?.minus(1)
                                ?: episodeIdx
                            onPlay(sourceIdx, ep, resumeStartSec)
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = ShapeMd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Text(
                        if (ui.playRecords.containsKey(current?.key.orEmpty())) "继续播放" else "立即播放",
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    onClick = { if (current != null) vm.toggleFavorite(current) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
                ) {
                    Icon(
                        if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "收藏",
                        tint = if (isFav) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { if (current != null) vm.toggleFollowing(current, totalEpisodes = episodes.size) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
                ) {
                    Icon(
                        if (isFollowing) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                        "追更",
                        tint = if (isFollowing) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 来源选择
        if (sources.size > 1) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("播放源", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sources.forEachIndexed { i, s ->
                            Surface(
                                onClick = { onSourceSelect(i) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (i == sourceIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (i == sourceIdx) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            ) {
                                Text(
                                    s.sourceName,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 选集
        if (episodes.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(20.dp))
                    Text("选集 · 共${episodes.size}集", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    items(episodes.indices.toList()) { i ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEpisodeSelect(i) }
                                .background(
                                    if (i == episodeIdx) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent,
                                    RoundedCornerShape(10.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                (episodesTitles.getOrNull(i) ?: "第${i + 1}集"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (i == episodeIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (i == episodeIdx) {
                                Text(
                                    "已选",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 简介
        if (group.desc.isNotBlank()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(20.dp))
                    Text("简介", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        group.desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

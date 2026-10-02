package com.xumitech.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.xumitech.tv.AppState
import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.model.PlayRecord
import com.xumitech.tv.ui.components.EmptyState
import com.xumitech.tv.ui.components.GradientProgressBar
import com.xumitech.tv.ui.components.ListSkeleton
import com.xumitech.tv.ui.theme.ScrimLight

/** 历史页：播放记录列表，点击续播，删除记录。 */
@Composable
fun HistoryScreen(
    appState: AppState,
    onOpenItem: (DoubanItem) -> Unit,
    onResume: (DoubanItem, Int, Long) -> Unit,
) {
    val records = appState.playRecords
    val loading = appState.historyLoading

    LaunchedEffect(Unit) {
        // 已有数据直接复用，避免切 tab 重复请求闪骨架
        if (records.isEmpty()) appState.loadPlayRecords()
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "观看历史",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        when {
            loading && records.isEmpty() -> ListSkeleton()
            records.isEmpty() -> EmptyState(
                "还没有观看记录",
                icon = Icons.Rounded.VideoLibrary,
                hint = "看过的影片会在这里记录进度",
            )
            else -> {
                // 按保存时间倒序
                val sorted = records.values.sortedByDescending { it.saveTime }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(sorted, key = { it.title + it.sourceName + it.index + it.saveTime }) { r ->
                        HistoryItem(
                            record = r,
                            onClick = {
                                onResume(
                                    DoubanItem(
                                        id = "",
                                        title = r.title,
                                        poster = r.cover,
                                        source = "",
                                        sourceName = r.sourceName,
                                        year = r.year,
                                    ),
                                    r.index - 1,
                                    r.playTime.toLong(),
                                )
                            },
                            onDelete = {
                                // 用 source+title 构造 key 删除
                                val key = findKey(records, r)
                                if (key != null) appState.removeRecord(key)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun findKey(records: Map<String, PlayRecord>, target: PlayRecord): String? {
    return records.entries.firstOrNull { (_, v) ->
        v.title == target.title && v.sourceName == target.sourceName &&
            v.index == target.index && v.saveTime == target.saveTime
    }?.key
}

@Composable
private fun HistoryItem(
    record: PlayRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val progress = if (record.totalTime > 0) {
        (record.playTime.toFloat() / record.totalTime).coerceIn(0f, 1f)
    } else 0f

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 封面
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
                    .background(ScrimLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayCircleOutline,
                    contentDescription = "继续播放",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = record.title,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (record.totalEpisodes > 1) {
                    "${record.sourceName} · 第${record.index}集 / 共${record.totalEpisodes}集"
                } else {
                    record.sourceName
                },
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(8.dp))
            // 进度条（公共组件）
            GradientProgressBar(progress, height = 4.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatTime(record.playTime)} / ${formatTime(record.totalTime)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = "删除记录",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun formatTime(sec: Int): String {
    if (sec < 0) return "00:00"
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

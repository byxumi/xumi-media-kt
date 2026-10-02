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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xumitech.tv.DetailTarget
import androidx.compose.foundation.clickable
import com.xumitech.tv.data.PlayRecord
import com.xumitech.tv.ui.components.SimpleEmpty
import com.xumitech.tv.ui.components.formatPlayTime
import com.xumitech.tv.ui.state.AppUiState
import com.xumitech.tv.ui.state.AppViewModel

/** 历史页:继续观看列表 + 单条删除。 */
@Composable
fun HistoryScreen(
    vm: AppViewModel,
    ui: AppUiState,
    modifier: Modifier = Modifier,
    onOpenDetail: (DetailTarget) -> Unit,
) {
    LaunchedEffect(Unit) {
        if (ui.playRecords.isEmpty()) vm.loadPlayRecords()
    }
    val records = ui.playRecords.values.sortedByDescending { it.saveTime }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "观看历史",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (records.isNotEmpty()) {
                TextButton(onClick = {
                    records.forEach { vm.removePlayRecord(playRecordKey(it)) }
                }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (records.isEmpty()) {
            SimpleEmpty(
                text = "还没有观看记录",
                subtitle = "看完的内容会出现在这里",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(records, key = { it.title + it.sourceName + it.saveTime }) { r: PlayRecord ->
                    HistoryRow(record = r, onClick = { onOpenDetail(recordTarget(r)) }, onDelete = { vm.removePlayRecord(playRecordKey(r)) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(record: PlayRecord, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableRow(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.xumitech.tv.ui.components.PosterTile(
            title = record.title,
            poster = record.cover,
            width = 84.dp,
            onClick = onClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "第${record.index}集 · ${record.sourceName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            // 进度条
            val progress = if (record.totalTime > 0) (record.playTime.toFloat() / record.totalTime).coerceIn(0f, 1f) else 0f
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                formatPlayTime(record.playTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.DeleteOutline,
                "删除记录",
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(
        Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    )

private fun recordTarget(r: PlayRecord) = DetailTarget(
    id = r.id.takeIf { it.isNotBlank() } ?: r.title,
    source = r.source.takeIf { it.isNotBlank() } ?: r.sourceName,
    title = r.title,
    poster = r.cover,
    year = r.year,
)

private fun playRecordKey(r: PlayRecord): String = if (r.source.isNotBlank() && r.id.isNotBlank()) "${r.source}+${r.id}" else r.title

package com.xumitech.tv.ui.screens

import com.xumitech.tv.DetailTarget

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xumitech.tv.data.DoubanItem
import com.xumitech.tv.ui.components.PosterTile
import com.xumitech.tv.ui.components.SimpleEmpty
import com.xumitech.tv.ui.components.SimpleError
import com.xumitech.tv.ui.state.AppUiState
import com.xumitech.tv.ui.state.AppViewModel

/** 发现页:分类筛选 + 网格。 */
@Composable
fun DiscoverScreen(
    vm: AppViewModel,
    ui: AppUiState,
    onClose: () -> Unit,
    onOpenDetail: (DetailTarget) -> Unit,
) {
    val kinds = listOf("movie" to "电影", "tv" to "剧集", "anime" to "动漫")
    var kind by remember { mutableIntStateOf(0) }

    LaunchedEffect(kind) {
        vm.loadDiscover(kind = kinds[kind].first)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
            Text(
                "发现",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        // 类型 chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(kinds.size) { i ->
                FilterChip(
                    selected = kind == i,
                    onClick = { kind = i },
                    label = { Text(kinds[i].second) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            ui.discoverLoading && ui.discoverItems.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            ui.discoverError != null -> SimpleError(text = ui.discoverError, onRetry = { vm.loadDiscover(kind = kinds[kind].first) }, modifier = Modifier.fillMaxSize())
            ui.discoverItems.isEmpty() -> SimpleEmpty(text = "该分类暂无内容", modifier = Modifier.fillMaxSize())
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(ui.discoverItems, key = { it.source + it.id }) { d: DoubanItem ->
                    PosterTile(
                        title = d.title,
                        poster = d.poster,
                        modifier = Modifier.fillMaxWidth(),
                        width = 0.dp, // 由网格约束决定宽度
                        rate = d.rate.takeIf { it.isNotBlank() },
                        onClick = { onOpenDetail(doubanTarget(d)) },
                    )
                }
            }
        }
    }
}

private fun doubanTarget(d: DoubanItem) = DetailTarget(id = d.id, source = d.source, title = d.title, poster = d.poster, year = d.year)

package com.xumitech.tv.ui.screens

import com.xumitech.tv.DetailTarget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xumitech.tv.data.Favorite
import com.xumitech.tv.ui.components.PosterTile
import com.xumitech.tv.ui.components.SimpleEmpty
import com.xumitech.tv.ui.components.SimpleError
import com.xumitech.tv.ui.state.AppUiState
import com.xumitech.tv.ui.state.AppViewModel

/** 收藏页:网格展示 + 取消收藏。 */
@Composable
fun FavoritesScreen(
    vm: AppViewModel,
    ui: AppUiState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onOpenDetail: (DetailTarget) -> Unit,
) {
    LaunchedEffect(Unit) {
        if (ui.favorites.isEmpty()) vm.loadFavorites()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
            Text(
                "我的收藏",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        when {
            ui.favorites.isEmpty() -> SimpleEmpty(
                text = "还没有收藏",
                subtitle = "在详情页点 ☆ 收藏影片",
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(ui.favorites.values.sortedByDescending { it.saveTime }.toList(), key = { it.title + it.sourceName }) { f: Favorite ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 两列网格
                        FavoriteRow(f = f, onOpen = { onOpenDetail(favoriteTarget(f)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(f: Favorite, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PosterTile(
            title = f.title,
            poster = f.cover,
            width = 96.dp,
            onClick = onOpen,
        )
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(f.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
            if (f.year.isNotBlank()) {
                Spacer(Modifier.padding(top = 2.dp))
                Text(f.year, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(f.sourceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

private fun favoriteTarget(f: Favorite) = DetailTarget(
    id = f.id.takeIf { it.isNotBlank() } ?: f.title,
    source = f.source.takeIf { it.isNotBlank() } ?: f.sourceName,
    title = f.title,
    poster = f.cover,
    year = f.year,
)

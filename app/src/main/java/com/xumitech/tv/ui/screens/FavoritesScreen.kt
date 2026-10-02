package com.xumitech.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xumitech.tv.AppState
import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.model.Favorite
import com.xumitech.tv.ui.components.EmptyState
import com.xumitech.tv.ui.components.GridSkeleton
import com.xumitech.tv.ui.components.PosterTile
import com.xumitech.tv.ui.theme.BadgeDelete

/** 收藏页：网格展示收藏，点开详情，角标删除。 */
@Composable
fun FavoritesScreen(
    appState: AppState,
    onOpenItem: (DoubanItem) -> Unit,
) {
    val favorites = appState.favorites
    val loading = appState.favLoading

    LaunchedEffect(Unit) {
        // 已有数据直接复用（AppState 内存缓存 + 详情页本地增删），避免切 tab 重复请求闪骨架
        if (favorites.isEmpty()) appState.loadFavorites()
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "我的收藏",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        when {
            loading && favorites.isEmpty() -> GridSkeleton()
            favorites.isEmpty() -> EmptyState(
                "还没有收藏",
                icon = Icons.Rounded.StarBorder,
                hint = "在详情页点收藏，喜欢的影片会出现在这里",
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(favorites.entries.toList(), key = { it.key }) { (key, fav) ->
                    FavoriteTile(
                        key = key,
                        fav = fav,
                        onClick = {
                            onOpenItem(
                                DoubanItem(
                                    id = "",
                                    title = fav.title,
                                    poster = fav.cover,
                                    source = "",
                                    sourceName = fav.sourceName,
                                    year = fav.year,
                                ),
                            )
                        },
                        onDelete = { appState.toggleFavorite(key, fav) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteTile(
    key: String,
    fav: Favorite,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    PosterTile(
        title = fav.title,
        poster = fav.cover,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        subtitle = fav.year,
        topEnd = {
            androidx.compose.material3.IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(34.dp)
                    .background(com.xumitech.tv.ui.theme.BadgeDelete, RoundedCornerShape(50)),
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "取消收藏",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
    )
}

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
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
import com.xumitech.tv.model.Favorite
import com.xumitech.tv.ui.components.EmptyState

/** 收藏页：网格展示收藏，点开详情，长按/角标删除。 */
@Composable
fun FavoritesScreen(
    appState: AppState,
    onOpenItem: (DoubanItem) -> Unit,
) {
    val favorites = appState.favorites

    LaunchedEffect(Unit) {
        appState.loadFavorites()
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "我的收藏",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        if (favorites.isEmpty()) {
            EmptyState("还没有收藏，去首页逛逛吧")
        } else {
            LazyVerticalGrid(
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
    Column(
        Modifier
            .clickable(onClick = onClick),
    ) {
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                AsyncImage(
                    model = fav.cover,
                    contentDescription = fav.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x66000000)),
                                startY = 400f,
                                endY = 600f,
                            ),
                        ),
                )
            }
            // 删除角标
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .background(Color(0x99000000), RoundedCornerShape(50)),
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "取消收藏",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = fav.title,
            maxLines = 1,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
        if (fav.year.isNotEmpty()) {
            Text(
                text = fav.year,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                maxLines = 1,
            )
        }
    }
}
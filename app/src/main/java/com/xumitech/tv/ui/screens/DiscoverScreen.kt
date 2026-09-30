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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xumitech.tv.AppState
import com.xumitech.tv.data.MoonTvApi
import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.ui.components.EmptyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 类型 Tab。 */
private data class KindTab(val label: String, val kind: String)

private val KIND_TABS = listOf(
    KindTab("电影", "movie"),
    KindTab("剧集", "tv"),
    KindTab("综艺", "show"),
    KindTab("动漫", "anime"),
)

/** 分类 chip。 */
private data class CategoryChip(val label: String, val category: String, val type: String)

private val CATEGORY_CHIPS = listOf(
    CategoryChip("热门", "", ""),
    CategoryChip("最新", "最新", ""),
    CategoryChip("高分", "高分", ""),
    CategoryChip("华语", "热门", "华语"),
    CategoryChip("欧美", "热门", "欧美"),
    CategoryChip("韩国", "热门", "韩国"),
    CategoryChip("日本", "热门", "日本"),
    CategoryChip("动作", "热门", "动作"),
    CategoryChip("喜剧", "热门", "喜剧"),
    CategoryChip("爱情", "热门", "爱情"),
    CategoryChip("科幻", "热门", "科幻"),
    CategoryChip("悬疑", "热门", "悬疑"),
    CategoryChip("恐怖", "热门", "恐怖"),
)

/** 分类榜单页（学 moontv douban 页）：类型Tab + 分类chips + 分页网格。 */
@Composable
fun DiscoverScreen(
    appState: AppState,
    onClose: () -> Unit,
    onOpenItem: (DoubanItem) -> Unit,
) {
    var kindIdx by remember { mutableIntStateOf(0) }
    var chipIdx by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<DoubanItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()

    val currentKind = KIND_TABS[kindIdx].kind
    val currentChip = CATEGORY_CHIPS[chipIdx]
    val scope = rememberCoroutineScope()

    fun load(reset: Boolean = true, newPage: Int = 0) {
        loading = true
        error = null
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    MoonTvApi.getDoubanCategories(
                        kind = currentKind,
                        category = currentChip.category,
                        type = currentChip.type,
                        limit = 30,
                        start = newPage * 30,
                    )
                }
                items = if (reset) list else items + list
                loading = false
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
                loading = false
            }
        }
    }

    LaunchedEffect(kindIdx, chipIdx) {
        page = 0
        load(true, 0)
    }

    // 滚动到底部自动加载更多
    LaunchedEffect(gridState, items.size) {
        snapshotFlow {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            lastVisible >= total - 6
        }.collect { nearEnd ->
            if (nearEnd && !loading && items.isNotEmpty()) {
                page += 1
                load(false, page)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
            }
            Text(
                "分类 · 榜单",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // 类型 Tab
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(KIND_TABS.size) { i ->
                val tab = KIND_TABS[i]
                val selected = i == kindIdx
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        )
                        .clickable { kindIdx = i }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(
                        tab.label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }

        // 分类 chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(CATEGORY_CHIPS.size) { i ->
                val chip = CATEGORY_CHIPS[i]
                val selected = i == chipIdx
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        )
                        .clickable { chipIdx = i }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        chip.label,
                        fontSize = 12.sp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }

        when {
            error != null && items.isEmpty() -> EmptyState(
                error!!,
                onRetry = { load(true, 0) },
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 12.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.id + it.source + it.title }) { item ->
                    // 复用 PosterTile 风格但网格化
                    GridPoster(item, onClick = { onOpenItem(item) })
                }
                // 加载更多
                if (loading) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(20.dp).width(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridPoster(
    item: DoubanItem,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            coil.compose.AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.rate.isNotEmpty() && item.rate != "0") {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(50))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        item.rate,
                        color = Color(0xFFFFC53D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = item.title,
            maxLines = 1,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}
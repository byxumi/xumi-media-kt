package com.xumitech.tv.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.xumitech.tv.model.SearchResult
import com.xumitech.tv.model.VideoGroup
import com.xumitech.tv.ui.components.EmptyState
import com.xumitech.tv.ui.components.PosterTile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 搜索结果条目（用于水平聚合显示）。 */
private data class SearchHit(
    val group: VideoGroup,
    val totalVideos: Int,
)

/**
 * 搜索页：输入 + 联想建议 + 历史 + 结果聚合。
 * 搜索结果按影片聚合（VideoGroup.group），横向卡片显示。
 */
@Composable
fun SearchScreen(
    appState: AppState,
    onClose: () -> Unit,
    onOpenItem: (DoubanItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var groups by remember { mutableStateOf<List<VideoGroup>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val history = appState.searchHistory
    var searched by remember { mutableStateOf(false) }
    var searchedQuery by remember { mutableStateOf("") }

    // 输入防抖联想（修改关键词后重新出联想；搜索完成且关键词未变时不上联想）
    LaunchedEffect(query) {
        if (query.isBlank() || (searched && query == searchedQuery)) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        suggestions = MoonTvApi.searchSuggestions(query)
    }

    fun runSearch(q: String) {
        searchedQuery = q
        doSearch(appState, q) {
            searching = it.first
            groups = it.second
            error = it.third
            searched = true
            appState.addSearchHistory(q)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 顶部搜索栏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
            }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索影片 / 剧集…") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "清空")
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50)),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "搜索",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        if (query.isNotBlank()) runSearch(query)
                    }
                    .padding(8.dp),
            )
        }

        when {
            searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            error != null -> EmptyState(
                text = error!!,
                onRetry = {
                    if (query.isNotBlank()) runSearch(query)
                },
            )

            // 联想优先：搜索后修改关键词时展示新联想，而不是旧的搜索结果
            query.isNotBlank() && suggestions.isNotEmpty() && query != searchedQuery -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Text(
                            "搜索建议",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(suggestions) { s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query = s
                                    runSearch(s)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(s, fontSize = 14.sp)
                        }
                    }
                }
            }

            groups.isNotEmpty() -> {
                // 搜索结果
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    item {
                        Text(
                            "找到 ${groups.size} 部影片",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(groups.size) { i ->
                        val g = groups[i]
                        SearchGroupRow(g, onOpenItem)
                    }
                }
            }

            // 联想建议（初始输入阶段：此时无搜索结果）
            query.isNotBlank() && suggestions.isNotEmpty() && groups.isEmpty() -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Text(
                            "搜索建议",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(suggestions) { s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query = s
                                    runSearch(s)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(s, fontSize = 14.sp)
                        }
                    }
                }
            }

            else -> {
                // 历史
                if (history.isEmpty()) {
                    EmptyState("搜索你想看的影视、剧集…")
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "搜索历史",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                        }
                        items(history) { h ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = h
                                        runSearch(h)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(h, fontSize = 14.sp)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { appState.deleteSearchHistory(h) }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun doSearch(
    appState: AppState,
    query: String,
    onResult: (Triple<Boolean, List<VideoGroup>, String?>) -> Unit,
) {
    appState.search(query) { results, err ->
        if (err != null) {
            onResult(Triple(false, emptyList(), err))
        } else {
            val grouped = VideoGroup.group(results ?: emptyList())
            onResult(Triple(false, grouped, null))
        }
    }
}

/** 搜索结果中的一部影片：横向源行。 */
@Composable
private fun SearchGroupRow(
    group: VideoGroup,
    onOpenItem: (DoubanItem) -> Unit,
) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenItem(
                        DoubanItem(
                            id = "",
                            title = group.title,
                            poster = group.poster,
                            source = "",
                            sourceName = "",
                            year = group.year,
                        ),
                    )
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                group.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${group.sources.size} 个源",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(group.sources) { src ->
                Column(
                    Modifier
                        .width(96.dp)
                        .clickable {
                            onOpenItem(
                                DoubanItem(
                                    id = src.id,
                                    title = group.title,
                                    poster = group.poster,
                                    source = src.source,
                                    sourceName = src.sourceName,
                                    year = group.year,
                                ),
                            )
                        },
                ) {
                    Box(
                        Modifier
                            .width(96.dp)
                            .height(144.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        coil.compose.AsyncImage(
                            model = group.poster,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (src.episodes.size > 1) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    "${src.episodes.size}集",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        src.sourceName,
                        maxLines = 1,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
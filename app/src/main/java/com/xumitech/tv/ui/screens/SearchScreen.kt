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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xumitech.tv.DetailTarget
import com.xumitech.tv.data.SearchResult
import com.xumitech.tv.ui.components.PosterTile
import com.xumitech.tv.ui.components.SimpleEmpty
import com.xumitech.tv.ui.components.SimpleError
import com.xumitech.tv.ui.state.AppUiState
import com.xumitech.tv.ui.state.AppViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 搜索页:联想 + 历史 + 结果。 */
@Composable
fun SearchScreen(
    vm: AppViewModel,
    ui: AppUiState,
    onClose: () -> Unit,
    onOpenDetail: (DetailTarget) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var debounce: Job? = remember { null }

    fun doSearch(q: String) {
        if (q.isBlank()) return
        active = true
        vm.search(q)
        vm.addSearchHistory(q.trim())
    }

    fun onQueryChange(q: String) {
        query = q
        if (q.isBlank()) {
            active = false
            suggestions = emptyList()
            return
        }
        active = false  // 输入新词时回到联想/历史态,避免卡在旧结果
        debounce?.cancel()
        debounce = scope.launch {
            delay(300)
            if (query.isNotBlank()) {
                vm.searchSuggestions(query) { s -> suggestions = s }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 搜索条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
            OutlinedTextField(
                value = query,
                onValueChange = ::onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索影片 / 剧集") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, "清空", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { doSearch(query) }),
            )
            IconButton(onClick = { doSearch(query) }) {
                Icon(Icons.Rounded.Search, "搜索", tint = MaterialTheme.colorScheme.primary)
            }
        }

        when {
            ui.searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            active -> {
                if (ui.searchError != null) {
                    SimpleError(text = ui.searchError, onRetry = { vm.search(query) }, modifier = Modifier.fillMaxSize())
                } else if (ui.searchResults.isEmpty()) {
                    SimpleEmpty(
                        text = "没有找到「${query}」",
                        subtitle = "换个关键词试试",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(ui.searchResults, key = { it.key }) { r: SearchResult ->
                            SearchResultRow(r, onClick = { onOpenDetail(searchTarget(r)) })
                        }
                    }
                }
            }

            query.isBlank() -> {
                // 联想 / 历史
                if (suggestions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    ) {
                        items(suggestions.size) { i ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onQueryChange(suggestions[i]) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                                Spacer(Modifier.size(12.dp))
                                Text(suggestions[i], style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                } else {
                    val history = ui.searchHistory
                    if (history.isEmpty()) {
                        SimpleEmpty(text = "搜索你想看的", subtitle = "支持影视、剧集、动漫", modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("搜索历史", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { history.forEach { vm.deleteSearchHistory(it) } }) {
                                        Icon(Icons.Rounded.DeleteOutline, "清空历史", tint = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                            items(history.size) { i ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { doSearch(history[i]) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(history[i], style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(r: SearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterTile(
            title = r.title,
            poster = r.poster,
            width = 72.dp,
            onClick = onClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (r.year.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(r.year, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                r.sourceName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

private fun searchTarget(r: SearchResult) = DetailTarget(id = r.id, source = r.source, title = r.title, poster = r.poster, year = r.year)

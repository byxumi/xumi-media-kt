package com.xumitech.tv.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.xumitech.tv.AppState
import com.xumitech.tv.data.MoonTvApi
import com.xumitech.tv.data.MoonTvApiHttpDataSource
import com.xumitech.tv.data.SourceScorer
import com.xumitech.tv.data.SpeedResult
import com.xumitech.tv.data.SpeedTester
import com.xumitech.tv.model.Favorite
import com.xumitech.tv.model.PlayRecord
import com.xumitech.tv.model.SearchResult
import com.xumitech.tv.model.VideoGroup
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 播放页：ExoPlayer 内核 + 测速优选 + 播放记录自动保存 + 源/选集切换。
 * - 进入时 >1 源则自动测速：清晰度40% + 速度40% + 延迟20% 评分优选
 * - 每 5 秒保存播放进度（与网页版一致）
 * - 后台/退出自动保存
 */
@Composable
fun PlayScreen(
    appState: AppState,
    group: VideoGroup,
    initialSource: SearchResult,
    initialEpisode: Int,
    startPositionSec: Long = 0,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sources by remember { mutableStateOf(group.sources) }
    var selectedSourceIdx by remember { mutableIntStateOf(sources.indexOfFirst { it.source == initialSource.source && it.id == initialSource.id }.coerceAtLeast(0)) }
    var selectedEpisode by remember { mutableIntStateOf(initialEpisode) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(true) }
    var testProgress by remember { mutableStateOf(0) }
    var testTotal by remember { mutableStateOf(0) }
    var preferredSource by remember { mutableStateOf<Pair<SearchResult, SpeedResult>?>(null) }
    var showPreferred by remember { mutableStateOf(false) }
    var isFav by remember { mutableStateOf(false) }
    var speedResults by remember { mutableStateOf<Map<String, SpeedResult>>(emptyMap()) }

    val currentSource = sources.getOrNull(selectedSourceIdx) ?: sources.first()
    val episodeUrl = currentSource.episodes.getOrNull(selectedEpisode)

    /** 当前源的防盗链 Referer（Source.referer：detail 优先，其次 api host）。 */
    fun refererOf(src: SearchResult): String =
        appState.sources.firstOrNull { it.key == src.source }?.referer ?: ""

    // 测速优选（>1 源时）
    LaunchedEffect(Unit) {
        testing = true
        testTotal = sources.size
        val results = mutableMapOf<String, SpeedResult>()
        // 分批并发（每批 4）
        for (batch in sources.chunked(4)) {
            val deferred = batch.map { src ->
                coroutineScope.async {
                    SpeedTester.testSpeed(
                        src.episodes.firstOrNull() ?: "",
                        refererOf(src),
                    )
                }
            }
            val batchResults = deferred.awaitAll()
            batch.forEachIndexed { i, src ->
                results[src.key] = batchResults.getOrNull(i) ?: SpeedResult.FAIL
                testProgress = results.size
            }
            speedResults = results.toMap()
        }
        testing = false
        if (sources.size > 1) {
            // 评分优选
            val scored = sources.mapIndexed { idx, src ->
                Triple(idx, src, SourceScorer.scoreOf(results[src.key] ?: SpeedResult.FAIL, results.values.toList()))
            }.sortedByDescending { it.third }
            val best = scored.firstOrNull { it.third >= 0 }
            if (best != null && best.first != selectedSourceIdx) {
                selectedSourceIdx = best.first
                preferredSource = best.second to (results[best.second.key] ?: SpeedResult.FAIL)
                showPreferred = true
                scope.launch {
                    delay(3000)
                    showPreferred = false
                }
            }
        }
    }

    // 创建播放器
    DisposableEffect(Unit) {
        val exo = ExoPlayer.Builder(context).build().also {
            it.repeatMode = Player.REPEAT_MODE_OFF
            it.playWhenReady = true
        }
        player = exo
        onDispose {
            // 退出前保存进度
            saveProgress(exo, currentSource, selectedEpisode, appState)
            exo.release()
            player = null
        }
    }

    // 播放当前源/集
    LaunchedEffect(currentSource, selectedEpisode) {
        val p = player ?: return@LaunchedEffect
        val url = episodeUrl ?: return@LaunchedEffect
        statusText = "加载 ${currentSource.sourceName}…"
        try {
            val factory = MoonTvApiHttpDataSource.factory(context, refererOf(currentSource))
            val hls = HlsMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(url))
            p.setMediaSource(hls)
            p.prepare()
            if (startPositionSec > 0) {
                p.seekTo(startPositionSec * 1000)
            }
            p.playWhenReady = true
            isPlaying = true
            statusText = null
        } catch (_: Exception) {
            statusText = "播放失败：$url"
        }
    }

    // 监听播放状态 + 每5秒保存
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && p.repeatMode == Player.REPEAT_MODE_OFF) {
                    // 自动连播下一集
                    if (selectedEpisode + 1 < currentSource.episodes.size) {
                        selectedEpisode += 1
                    }
                }
            }
        })
        while (true) {
            delay(5000)
            if (!isPlaying) continue
            val pos = p.currentPosition
            val dur = p.duration
            if (dur <= 0 || pos < 1000) continue
            saveProgress(p, currentSource, selectedEpisode, appState)
        }
    }

    // 收藏状态
    LaunchedEffect(currentSource, appState.favorites) {
        isFav = appState.favorites.containsKey(currentSource.key)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // 视频画布（16:9）
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
        ) {
            val p = player
            if (p != null && episodeUrl != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = true
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            player = p
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // 加载状态遮罩
            if (statusText != null || testing) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (testing) {
                        SpeedTestOverlay(
                            progress = testProgress,
                            total = testTotal,
                        )
                    } else if (statusText != null) {
                        Text(
                            statusText!!,
                            color = Color.White,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        // 信息区
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        group.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${currentSource.sourceName} · ${currentSource.episodeTitle(selectedEpisode)}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
                // 返回
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                    )
                }
            }

            // 已优选横幅
            AnimatedVisibility(
                visible = showPreferred,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val pref = preferredSource
                if (pref != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                        Color(0xFF0EA5E9).copy(alpha = 0.75f),
                                    ),
                                ),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "⚡ 已优选：${pref.first.sourceName} · ${pref.second.quality} · ${pref.second.speedText}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // 测速详情（滚动查看）
            if (speedResults.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "源测速",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(6.dp))
                sources.forEachIndexed { idx, src ->
                    val r = speedResults[src.key] ?: SpeedResult.FAIL
                    val selected = idx == selectedSourceIdx
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else Color.White.copy(alpha = 0.05f),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { selectedSourceIdx = idx }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            src.sourceName,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        if (r.ok) {
                            Text(
                                "${r.quality} ${r.speedText}",
                                color = Color(0xFF2ED573),
                                fontSize = 11.sp,
                            )
                        } else {
                            Text(
                                "失败",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        androidx.compose.material3.Icon(
                            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFav) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    appState.toggleFavorite(
                                        src.key,
                                        Favorite(
                                            title = src.title,
                                            sourceName = src.sourceName,
                                            cover = src.poster,
                                            year = src.year,
                                            saveTime = System.currentTimeMillis(),
                                            searchTitle = src.title,
                                        ),
                                    )
                                },
                        )
                    }
                }
            }

            // 选集
            if (currentSource.episodes.size > 1) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "选集",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentSource.episodes.size) { i ->
                        val selected = i == selectedEpisode
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.1f),
                                )
                                .clickable { selectedEpisode = i }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                currentSource.episodeTitle(i),
                                color = if (selected) Color.White
                                else Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // 顶部返回条（视频区覆盖）
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x99000000), Color.Transparent),
                    ),
                )
                .padding(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                    )
                }
                Text(
                    group.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 测速覆盖层（液态玻璃卡片 + 进度）。 */
@Composable
private fun SpeedTestOverlay(
    progress: Int,
    total: Int,
) {
    val pct = if (total > 0) (progress * 100 / total) else 0
    Column(
        Modifier
            .padding(24.dp)
            .background(Color(0xFF14161F).copy(alpha = 0.92f), RoundedCornerShape(26.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(46.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "正在聚合播放源 · 测速优选",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "已测速 ${progress}/${total} 个源 · ${pct}%",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(10.dp))
        // 进度条
        Box(
            Modifier
                .width(200.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pct / 100f)
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF0EA5E9),
                            ),
                        ),
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

/** 保存播放进度（含后台/退出）。 */
private fun saveProgress(
    player: Player,
    source: SearchResult,
    episode: Int,
    appState: AppState,
) {
    try {
        val pos = player.currentPosition
        val dur = player.duration
        if (dur <= 0 || pos < 1000) return
        appState.saveRecord(
            source.key,
            PlayRecord(
                title = source.title,
                sourceName = source.sourceName,
                cover = source.poster,
                year = source.year,
                index = episode + 1,
                totalEpisodes = source.episodes.size,
                playTime = (pos / 1000).toInt(),
                totalTime = (dur / 1000).toInt(),
                saveTime = System.currentTimeMillis(),
                searchTitle = source.title,
            ),
        )
    } catch (_: Exception) {}
}
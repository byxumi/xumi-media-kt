package com.xumitech.tv.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.xumitech.tv.DetailTarget
import com.xumitech.tv.PlayRequest
import com.xumitech.tv.data.MoonTvApiHttpDataSource
import com.xumitech.tv.data.SearchResult
import com.xumitech.tv.data.SourceScorer
import com.xumitech.tv.data.SpeedResult
import com.xumitech.tv.data.SpeedTester
import com.xumitech.tv.data.VideoGroup
import com.xumitech.tv.ui.state.AppUiState
import com.xumitech.tv.ui.state.AppViewModel
import com.xumitech.tv.ui.theme.Accent
import com.xumitech.tv.ui.theme.BrandSky
import com.xumitech.tv.ui.theme.CinemaBlack
import com.xumitech.tv.ui.theme.Success
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** 播放页:黑底沉浸式视频 + 信息区(源/选集/倍速/测速)。 */
@Composable
fun PlayScreen(
    vm: AppViewModel,
    ui: AppUiState,
    request: PlayRequest,
    onClose: () -> Unit,
    onOpenDetail: (DetailTarget, Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val group = request.group
    val sources = remember(group) { group.sources }
    var selectedSourceIdx by remember { mutableIntStateOf(
        request.sourceIndex.coerceIn(0, sources.size - 1),
    ) }
    var selectedEpisode by remember { mutableIntStateOf(request.episodeIndex) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var testProgress by remember { mutableIntStateOf(0) }
    var testTotal by remember { mutableIntStateOf(0) }
    var preferredSource by remember { mutableStateOf<Pair<SearchResult, SpeedResult>?>(null) }
    var showPreferred by remember { mutableStateOf(false) }
    var speedResults by remember { mutableStateOf<Map<String, SpeedResult>>(emptyMap()) }
    var isFullscreen by remember { mutableStateOf(false) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isFav by remember { mutableStateOf(false) }

    val currentSource = sources.getOrNull(selectedSourceIdx) ?: sources.firstOrNull()

    fun refererOf(src: SearchResult): String =
        ui.sources.firstOrNull { it.key == src.source }?.referer ?: ""

    val episodeUrl: String? = currentSource?.episodes?.getOrNull(selectedEpisode)

    // ---------- 全屏辅助 ----------
    fun findActivity(c: Context): Activity? {
        var cur = c
        while (cur is ContextWrapper) {
            if (cur is Activity) return cur
            cur = cur.baseContext
        }
        return null
    }

    fun enterFullscreen() {
        val act = findActivity(context) ?: return
        isFullscreen = true
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        act.window?.let { w ->
            WindowCompat.getInsetsController(w, w.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    fun exitFullscreen() {
        if (!isFullscreen) return
        isFullscreen = false
        val act = findActivity(context) ?: return
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        act.window?.let { w ->
            WindowCompat.getInsetsController(w, w.decorView).show(
                WindowInsetsCompat.Type.systemBars(),
            )
        }
    }

    fun toggleFullscreen() {
        if (isFullscreen) exitFullscreen() else enterFullscreen()
    }

    // ---------- 播放器 ----------
    DisposableEffect(Unit) {
        val exo = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
        player = exo
        onDispose {
            try {
                val p = player ?: return@onDispose
                val pos = p.currentPosition
                val dur = p.duration
                val src = currentSource ?: return@onDispose
                if (dur > 0 && pos >= 1000) {
                    vm.savePlayRecord(
                        item = src,
                        index = selectedEpisode + 1,
                        playTime = (pos / 1000).toInt(),
                        totalTime = (dur / 1000).toInt(),
                    )
                }
                vm.syncFollowingProgress(src, selectedEpisode)
            } catch (_: Exception) {}
            player?.release()
            player = null
            exitFullscreen()
        }
    }

    // 播放当前源/集
    LaunchedEffect(currentSource, selectedEpisode) {
        val p = player ?: return@LaunchedEffect
        val url = episodeUrl ?: return@LaunchedEffect
        val src = currentSource ?: return@LaunchedEffect
        statusText = "加载 ${src.sourceName}…"
        try {
            val factory = MoonTvApiHttpDataSource.factory(context, refererOf(src))
            val hls = HlsMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(url))
            p.setMediaSource(hls)
            p.prepare()
            if (request.startSec > 0) {
                p.seekTo(request.startSec * 1000)
            }
            p.playWhenReady = true
            isPlaying = true
            statusText = null
        } catch (_: Exception) {
            statusText = "播放失败，请重试或切换其他源"
        }
    }

    // 倍速应用
    LaunchedEffect(playbackSpeed) {
        player?.setPlaybackParameters(PlaybackParameters(playbackSpeed, 1f))
    }

    // 监听播放状态 + 每5秒保存进度
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && p.repeatMode == Player.REPEAT_MODE_OFF) {
                    val src = currentSource
                    if (src != null && selectedEpisode + 1 < src.episodes.size) {
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
            val src = currentSource ?: continue
            if (dur <= 0 || pos < 1000) continue
            vm.savePlayRecord(
                item = src,
                index = selectedEpisode + 1,
                playTime = (pos / 1000).toInt(),
                totalTime = (dur / 1000).toInt(),
            )
        }
    }

    // 收藏状态
    LaunchedEffect(currentSource, ui.favorites) {
        isFav = currentSource != null && ui.favorites.containsKey(currentSource.key)
    }

    // 测速优选(仅多源时)
    LaunchedEffect(sources) {
        if (sources.size <= 1) return@LaunchedEffect
        testing = true
        testTotal = sources.size
        testProgress = 0
        speedResults = emptyMap()
        val results = linkedMapOf<String, SpeedResult>()
        try {
            for (batch in sources.chunked(4)) {
                batch.map { src ->
                    scope.async {
                        src.key to SpeedTester.testSpeed(
                            src.episodes.firstOrNull() ?: "",
                            refererOf(src),
                        )
                    }
                }.forEach { deferred ->
                    val (key, r) = deferred.await()
                    results[key] = r
                    testProgress = results.size
                }
            }
            speedResults = results
            val scored = sources.map { src ->
                Triple(
                    src,
                    results[src.key] ?: SpeedResult.FAIL,
                    SourceScorer.scoreOf(
                        results[src.key] ?: SpeedResult.FAIL,
                        results.values.toList(),
                    ),
                )
            }.sortedByDescending { it.third }
            val best = scored.firstOrNull()
            if (best != null && best.second.ok && best.first.key != currentSource?.key) {
                val idx = sources.indexOf(best.first)
                if (idx >= 0) {
                    selectedSourceIdx = idx
                    preferredSource = best.first to best.second
                    showPreferred = true
                    scope.launch {
                        delay(3000)
                        showPreferred = false
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            testing = false
        }
    }

    // 全屏时:系统返回键先退出全屏
    BackHandler(enabled = isFullscreen) {
        exitFullscreen()
    }

    // ---------- 布局:视频在上、信息区在下 ----------
    Column(Modifier.fillMaxSize().background(CinemaBlack)) {
        // 视频画布
        Box(
            Modifier
                .fillMaxWidth()
                .then(if (isFullscreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f))
                .background(CinemaBlack),
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
                            val gd = GestureDetector(
                                ctx,
                                object : GestureDetector.SimpleOnGestureListener() {
                                    override fun onDoubleTap(e: MotionEvent): Boolean {
                                        toggleFullscreen()
                                        return true
                                    }
                                },
                            )
                            setOnTouchListener { _, ev -> gd.onTouchEvent(ev); false }
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
                        .background(CinemaBlack.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (testing) {
                        SpeedTestOverlay(progress = testProgress, total = testTotal)
                    } else if (statusText != null) {
                        Text(
                            statusText!!,
                            color = Color.White,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // 顶部控制条
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            listOf(CinemaBlack.copy(alpha = 0.85f), Color.Transparent),
                        ),
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(onClick = { if (isFullscreen) exitFullscreen() else onClose() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (isFullscreen) "退出全屏" else "返回",
                            tint = Color.White,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            group.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isFullscreen && currentSource != null) {
                            Text(
                                "${currentSource.sourceName} · ${currentSource.episodeTitle(selectedEpisode)}",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    // 倍速按钮(胶囊)
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { speedMenuOpen = !speedMenuOpen }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Speed,
                            contentDescription = "倍速",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            formatSpeed(playbackSpeed),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    IconButton(onClick = { toggleFullscreen() }) {
                        Icon(
                            if (isFullscreen) Icons.Rounded.FullscreenExit
                            else Icons.Rounded.Fullscreen,
                            contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                            tint = Color.White,
                        )
                    }
                }
            }

            // 倍速面板(玻璃卡片)
            androidx.compose.animation.AnimatedVisibility(
                visible = speedMenuOpen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 12.dp),
            ) {
                Column(
                    Modifier
                        .background(
                            Color(0xFF181B23).copy(alpha = 0.9f),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        "播放倍速",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SPEED_OPTIONS.forEach { s ->
                            val selected = s == playbackSpeed
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else Color.White.copy(alpha = 0.1f),
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        playbackSpeed = s
                                        speedMenuOpen = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    formatSpeed(s),
                                    color = if (selected) Color.White
                                    else Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }

            // 全屏底部信息条
            if (isFullscreen) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, CinemaBlack.copy(alpha = 0.85f)),
                            ),
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (currentSource != null) {
                        Text(
                            "${currentSource.sourceName} · ${currentSource.episodeTitle(selectedEpisode)} · 双击退出全屏",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // 信息区(全屏时隐藏)
        if (!isFullscreen && currentSource != null) {
            val src = currentSource
            Column(
                Modifier
                    .weight(1f)
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
                            "${src.sourceName} · ${src.episodeTitle(selectedEpisode)}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
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
                                            BrandSky.copy(alpha = 0.75f),
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

                // 测速详情(滚动查看)
                if (speedResults.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "源测速",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    sources.forEachIndexed { idx, s ->
                        val r = speedResults[s.key] ?: SpeedResult.FAIL
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
                                s.sourceName,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            if (r.ok) {
                                Text(
                                    "${r.quality} ${r.speedText}",
                                    color = Success,
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
                            Icon(
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
                                        vm.toggleFavorite(s)
                                    },
                            )
                        }
                    }
                }

                // 选集
                if (src.episodes.size > 1) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "选集",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(src.episodes.size) { i ->
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
                                    src.episodeTitle(i),
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
        }
    }
}

/** 倍速显示文本(1.0x / 1.25x / 0.5x)。 */
private fun formatSpeed(speed: Float): String {
    val v = if (speed == speed.toInt().toFloat()) speed.toInt() else speed
    return "${v}x"
}

/** 测速覆盖层(玻璃卡片 + 进度)。 */
@Composable
private fun SpeedTestOverlay(
    progress: Int,
    total: Int,
) {
    val pct = if (total > 0) (progress * 100 / total) else 0
    Column(
        Modifier
            .padding(24.dp)
            .background(Color(0xFF101218).copy(alpha = 0.92f), RoundedCornerShape(26.dp))
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
            "已测速 ${progress}/${total} 个源 · $pct%",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(10.dp))
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
                                BrandSky,
                            ),
                        ),
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

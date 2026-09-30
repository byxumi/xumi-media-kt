package com.xumitech.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.model.PlayRecord

/** 海报卡片（学 Flutter PosterImage：圆角 + 底部渐变 + 评分角标）。 */
@Composable
fun PosterTile(
    item: DoubanItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 112.dp,
) {
    Column(
        modifier
            .width(width)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .width(width)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // 底部渐变遮罩
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x66000000)),
                            startY = 400f,
                            endY = 600f,
                        ),
                    ),
            )
            // 评分角标
            if (item.rate.isNotEmpty() && item.rate != "0") {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = item.rate,
                        color = Color(0xFFFFC53D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            maxLines = 1,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.year.isNotEmpty()) {
            Text(
                text = item.year,
                maxLines = 1,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
        }
    }
}

/** 继续观看卡片（播放记录 + 进度条）。 */
@Composable
fun ContinueWatchingCard(
    record: PlayRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (record.totalTime > 0) {
        (record.playTime.toFloat() / record.totalTime).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier
            .width(168.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .width(168.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AsyncImage(
                model = record.cover,
                contentDescription = record.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // 播放图标覆盖
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000)),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                    contentDescription = "继续播放",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp),
                )
            }
            // 集数角标
            if (record.totalEpisodes > 1) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "第${record.index}集",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = record.title,
            maxLines = 1,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
        // 进度条
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(3.dp)
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
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatPlayTime(record.playTime),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}

private fun formatPlayTime(sec: Int): String {
    if (sec <= 0) return "未开始"
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return if (h > 0) "已观看 ${h}小时${m}分" else "已观看 ${m}分钟"
}

/** 追更卡片（学网页端 Following）：已看 N / 共 M 集 + 未看角标。 */
@Composable
fun FollowingCard(
    title: String,
    poster: String,
    watchedEpisodes: Int,
    totalEpisodes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unwatched = (totalEpisodes - watchedEpisodes).coerceAtLeast(0)
    Column(
        modifier
            .width(110.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .width(110.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AsyncImage(
                model = poster,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x66000000)),
                            startY = 400f,
                            endY = 600f,
                        ),
                    ),
            )
            // 未看角标（学 Flutter: '$unwatched 未看'）
            if (unwatched > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xE6142B3F), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "$unwatched 未看",
                        color = Color(0xFFFFC53D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            maxLines = 1,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "看完 $watchedEpisodes/$totalEpisodes 集",
            maxLines = 1,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}

/** 今日新更卡片（学 Flutter _TodayUpdated）：更新+新集数角标。 */
@Composable
fun TodayUpdatedCard(
    title: String,
    poster: String,
    sourceName: String,
    newEpisodes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .width(128.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .width(128.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AsyncImage(
                model = poster,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x66000000)),
                            startY = 400f,
                            endY = 600f,
                        ),
                    ),
            )
            // 更新角标（学 Flutter: '更新+N'）
            if (newEpisodes > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xE6007A2D), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "更新+$newEpisodes",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            maxLines = 1,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = sourceName,
            maxLines = 1,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}

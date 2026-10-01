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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.model.PlayRecord
import com.xumitech.tv.ui.theme.BadgeBlue
import com.xumitech.tv.ui.theme.BadgeGreen
import com.xumitech.tv.ui.theme.Gold
import com.xumitech.tv.ui.theme.Scrim
import com.xumitech.tv.ui.theme.ScrimHeavy
import com.xumitech.tv.ui.theme.ScrimLight
import com.xumitech.tv.ui.theme.SkyBlue

/** 海报底部渐变遮罩（PosterTile 家族共用）。 */
private fun Modifier.posterScrim(): Modifier = this.background(
    Brush.verticalGradient(
        colors = listOf(Color.Transparent, Scrim),
        startY = 400f,
        endY = 600f,
    ),
)

/** 卡片通用小角标。 */
@Composable
private fun TileBadge(
    text: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** 海报卡片（统一 4 变体: PosterTile / GridPoster / FavoriteTile / 搜索源卡）。 */
@Composable
fun PosterTile(
    title: String,
    poster: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 112.dp,
    rate: String? = null,
    badge: (@Composable () -> Unit)? = null,
    topEnd: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
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
                model = poster,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().posterScrim())
            if (rate != null && rate.isNotEmpty() && rate != "0") {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(ScrimHeavy, RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = rate,
                        color = Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (topEnd != null) {
                Box(Modifier.align(Alignment.TopEnd).padding(2.dp)) { topEnd() }
            }
            if (badge != null) {
                Box(Modifier.align(Alignment.BottomEnd).padding(6.dp)) { badge() }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
        }
    }
}

/** 兼容旧签名: 由 DoubanItem 驱动。 */
@Composable
fun PosterTile(
    item: DoubanItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 112.dp,
) {
    PosterTile(
        title = item.title,
        poster = item.poster,
        onClick = onClick,
        modifier = modifier,
        width = width,
        rate = item.rate,
        subtitle = item.year,
    )
}

/** 公共渐变进度条（ContinueWatchingCard / HistoryItem / Detail / Play 共用）。 */
@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    corner: Dp = 2.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(corner)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .background(
                    Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.primary, SkyBlue),
                    ),
                    RoundedCornerShape(corner),
                ),
        )
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
                    .background(ScrimLight),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "继续播放",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp),
                )
            }
            // 集数角标
            if (record.totalEpisodes > 1) {
                TileBadge(
                    text = "第${record.index}集",
                    background = ScrimHeavy,
                    textColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = record.title,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        GradientProgressBar(progress)
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatPlayTime(record.playTime),
            style = MaterialTheme.typography.labelSmall,
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
    PosterTile(
        title = title,
        poster = poster,
        onClick = onClick,
        modifier = modifier,
        width = 110.dp,
        badge = if (unwatched > 0) {
            {
                TileBadge(
                    text = "$unwatched 未看",
                    background = BadgeBlue,
                    textColor = Gold,
                )
            }
        } else null,
        subtitle = "看完 $watchedEpisodes/$totalEpisodes 集",
    )
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
    PosterTile(
        title = title,
        poster = poster,
        onClick = onClick,
        modifier = modifier,
        width = 128.dp,
        badge = if (newEpisodes > 0) {
            {
                TileBadge(
                    text = "更新+$newEpisodes",
                    background = BadgeGreen,
                    textColor = Color.White,
                )
            }
        } else null,
        subtitle = sourceName,
    )
}

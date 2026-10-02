package com.xumitech.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.xumitech.tv.data.DoubanItem
import com.xumitech.tv.data.Favorite
import com.xumitech.tv.data.Following
import com.xumitech.tv.data.PlayRecord
import com.xumitech.tv.data.TodayUpdatedItem
import com.xumitech.tv.ui.theme.Accent
import com.xumitech.tv.ui.theme.AccentBright
import com.xumitech.tv.ui.theme.Danger
import com.xumitech.tv.ui.theme.ShapeMd
import com.xumitech.tv.ui.theme.ShapeXs
import com.xumitech.tv.ui.theme.Success

/** 海报 2:3,圆角 12,底部渐变遮罩 + 评分角标 + 按压鼓胀。 */
@Composable
fun PosterTile(
    title: String,
    poster: String,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 112.dp,
    rate: String? = null,
    badge: String? = null,
    badgeColor: Color = Accent,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableFloatStateOf(0f) }
    val scale by animateFloatAsState(
        targetValue = if (pressed > 0f) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "posterScale",
    )
    val wMod = if (width > 0.dp) Modifier.width(width) else Modifier
    Column(
        modifier = modifier.then(wMod),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .then(wMod)
                .aspectRatio(2f / 3f)
                .scale(scale)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(title) {
                    detectTapGestures(
                        onPress = {
                            pressed = 1f
                            tryAwaitRelease()
                            pressed = 0f
                        },
                        onTap = { onClick() },
                    )
                },
        ) {
            AsyncImage(
                model = poster,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // 底部渐变遮罩(文字可读性)
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
            // 评分角标(右下)
            if (rate != null && rate.isNotBlank()) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        rate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Accent,
                    )
                }
            }
            // 角标(左上)
            if (badge != null) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 继续观看卡(16:9 横卡,遮罩 + 播放箭头 + 进度条)。 */
@Composable
fun ContinueWatchingCard(
    record: PlayRecord,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val progress = if (record.totalTime > 0) (record.playTime.toFloat() / record.totalTime).coerceIn(0f, 1f) else 0f
    Column(
        modifier = modifier.width(168.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(168.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = record.cover,
                contentDescription = record.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000)),
            )
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "继续播放",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp),
            )
            // 集数角标
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    "第${record.index}集",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            // 进度条(底部)
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary, AccentBright),
                            ),
                        ),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            record.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "已观看 ${formatPlayTime(record.playTime)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 追更卡(110dp 竖卡,未看角标)。 */
@Composable
fun FollowingCard(
    following: Following,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val unwatched = (following.totalEpisodes - following.watchedEpisodes).coerceAtLeast(0)
    Column(
        modifier = modifier.width(110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(110.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = following.cover,
                contentDescription = following.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (unwatched > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xE6142B3F))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "$unwatched 未看",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Accent,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            following.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "看完 ${following.watchedEpisodes}/${following.totalEpisodes} 集",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 今日新更卡(128dp 竖卡,更新角标)。 */
@Composable
fun TodayUpdatedCard(
    item: TodayUpdatedItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.width(128.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(128.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.newEpisodes > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xE6007A2D))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "更新+${item.newEpisodes}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 时间格式化:秒 → "X小时Y分" / "X分钟"。 */
fun formatPlayTime(seconds: Int): String {
    if (seconds <= 0) return "未观看"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}小时${m}分" else "${m}分钟"
}

package com.xumitech.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.xumitech.tv.ui.theme.LiquidAccentDark
import com.xumitech.tv.ui.theme.LiquidAccentLight
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlin.math.abs
import kotlin.math.sign

data class NavTab(
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector,
)

/**
 * 真·液态玻璃底部导航栏 —— 完整移植官方 Kyant0/AndroidLiquidGlass LiquidBottomTabs 动效。
 *
 * 官方目录样例的动效引擎逐一对齐：
 * - **[DampedDragAnimation]**: 液滴拖拽 —— 按下鼓胀、横向拖动跟随、松手惯性回弹、
 *   速度参与 scale 形变(液态拉长), 目标 Tab 弹性吸附
 * - **[InteractiveHighlight]**: 按压径向高亮(触点光晕, RuntimeShader)
 * - 三层叠绘: 玻璃面板(折射+模糊+增艳) / 采样层(录制背后内容) / 液态指示器滴
 * - **动效图标**: 选中图标弹性放大 + 径向发光 + 颜色渐变过渡
 */
@Composable
fun LiquidNavBar(
    tabs: List<NavTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inDark = isSystemInDarkTheme()
    val accent = if (inDark) LiquidAccentDark else LiquidAccentLight
    val containerColor =
        if (inDark) Color(0xFF121212).copy(alpha = 0.4f)
        else Color(0xFFFAFAFA).copy(alpha = 0.4f)
    val textColor =
        if (inDark) Color(0xCCF2F2F2) else Color(0xCC111111)

    val tabsBackdrop = rememberLayerBackdrop()
    val animationScope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .height(66.dp),
    ) {
        val density = LocalDensity.current
        // 官方: tab 宽度按 (总宽 - 两侧 4dp 边距) / 数量
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabs.size
        }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val lastIndex = (tabs.size - 1).coerceAtLeast(0)

        // ---- 液滴拖拽引擎(官方 DampedDragAnimation) ----
        val damped = remember(animationScope, tabs.size) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.toFloat(),
                valueRange = 0f..lastIndex.toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.12f,
                onDragStarted = {},
                onDragStopped = {
                    val target = targetValue.toInt().coerceIn(0, lastIndex)
                    onSelected(target)
                    animateToValue(target.toFloat())
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (value + dragAmount.x / tabWidth)
                            .coerceIn(0f, lastIndex.toFloat()),
                    )
                },
            )
        }

        // 外部索引变化 → 液滴弹性吸附到对应 Tab
        LaunchedEffect(selectedIndex, damped) {
            snapshotFlow { selectedIndex }
                .drop(1)
                .collectLatest { index ->
                    damped.animateToValue(index.toFloat())
                    onSelected(index)
                }
        }

        // 面板轻微反方向位移(官方 panelOffset 简化版: 拖拽时玻璃面板微微后退)
        val panelOffsetPx = with(density) {
            val frac = damped.progress - damped.progress.toInt()
            4f.dp.toPx() * frac.sign * EaseOut.transform(abs(frac))
        }

        // 0) 采样层: 录制背后内容(不可见)
        Box(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .layerBackdrop(tabsBackdrop),
        )

        // 1) 玻璃面板: 折射背后内容
        Box(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .graphicsLayer { translationX = panelOffsetPx }
                .drawBackdrop(
                    backdrop = tabsBackdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(10.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx())
                    },
                    onDrawSurface = { drawRect(containerColor) },
                ),
        )

        // 2) 液态指示器滴: 随液滴拖拽平移 + 按压鼓胀 + 速度形变 + 径向高光
        val highlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, offset ->
                    androidx.compose.ui.geometry.Offset(
                        if (isLtr) (damped.value + 0.5f) * tabWidth
                        else size.width - (damped.value + 0.5f) * tabWidth,
                        size.height / 2f,
                    )
                },
            )
        }

        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth(1f / tabs.size)
                .height(66.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) damped.value * tabWidth + panelOffsetPx
                        else (constraints.maxWidth.toFloat() - 8f.dp.toPx()) +
                            (lastIndex - damped.value) * tabWidth + panelOffsetPx
                    scaleX = damped.scaleX
                    scaleY = damped.scaleY
                    // 速度形变: 快速拖动时液滴被拉长
                    val velocity = damped.velocity / 10f
                    scaleX /= (1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f))
                    scaleY *= (1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f))
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        val progress = damped.pressProgress
                        lens(10.dp.toPx() * progress, 14.dp.toPx() * progress, chromaticAberration = true)
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = damped.pressProgress)
                    },
                    shadow = {
                        Shadow(alpha = damped.pressProgress)
                    },
                    innerShadow = {
                        InnerShadow(radius = 8.dp * damped.pressProgress, alpha = damped.pressProgress)
                    },
                    onDrawSurface = {
                        val progress = damped.pressProgress
                        // 未按压时半透明玻璃底, 按压时渐变为品牌色(液态充满)
                        drawRect(
                            if (inDark) Color.White.copy(0.08f) else Color.Black.copy(0.08f),
                            alpha = 1f - progress,
                        )
                        drawRect(accent.copy(alpha = 0.35f + 0.45f * progress))
                    },
                )
                .then(highlight.modifier)
                .then(damped.modifier)
        )

        // 3) Tab 内容层(图标 + 文字, 可点击)
        Row(
            Modifier
                .fillMaxWidth()
                .height(66.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, tab ->
                LiquidNavItem(
                    tab = tab,
                    selected = i == selectedIndex,
                    accent = accent,
                    textColor = textColor,
                    modifier = Modifier
                        .weight(1f)
                        .height(66.dp)
                        .clip(Capsule())
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                        ) {
                            if (i != selectedIndex) onSelected(i)
                        },
                )
            }
        }
    }
}

/** 单个导航项: 动效图标(选中弹性放大 + 发光 + 颜色渐变) + 文字。 */
@Composable
private fun RowScope.LiquidNavItem(
    tab: NavTab,
    selected: Boolean,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val iconScale = remember { Animatable(if (selected) 1f else 0.88f) }
    LaunchedEffect(selected) {
        iconScale.animateTo(
            if (selected) 1f else 0.88f,
            spring(dampingRatio = 0.5f, stiffness = 400f),
        )
    }
    val iconColor by animateColorAsState(
        targetValue = if (selected) accent else textColor.copy(alpha = 0.6f),
        animationSpec = tween(300),
        label = "iconColor",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) accent else textColor.copy(alpha = 0.6f),
        animationSpec = tween(300),
        label = "labelColor",
    )

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 选中发光底(径向渐变光晕)
            if (selected) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                                radius = 34f,
                            ),
                        ),
                )
            }
            Icon(
                imageVector = if (selected) tab.activeIcon else tab.icon,
                contentDescription = tab.label,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale.value),
            )
        }
        Text(
            text = tab.label,
            color = labelColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

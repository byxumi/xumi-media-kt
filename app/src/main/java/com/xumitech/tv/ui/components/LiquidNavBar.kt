package com.xumitech.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class NavTab(
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector,
)

/**
 * 真·液态玻璃底部导航栏（AndroidLiquidGlass / Kyant0 backdrop 库）。
 *
 * 完整复刻 LiquidBottomTabs 视觉语言：
 * - **玻璃面板**：layerBackdrop 录制背后内容 → lens(22dp) 折射 + blur(10dp) + vibrancy 增艳
 * - **选中指示器**：lens + chromaticAberration + Highlight + Shadow + InnerShadow，
 *   随选中 spring 弹性流动（dampingRatio 0.68 轻微超调 = 液态感）
 * - **按压鼓胀**（学 DampedDragAnimation）：按下时指示器放大 1.08x + 高光增强，
 *   松手弹簧回弹（pressProgress → scale）
 * - **动效图标**：选中图标 spring 弹性放大 + 颜色渐变过渡 + 文字加粗渐变，
 *   未选中图标微缩放回
 */
@Composable
fun LiquidNavBar(
    tabs: List<NavTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = !isSystemInDarkTheme()
    val accent = if (isLight) LiquidAccentLight else LiquidAccentDark
    val containerColor =
        if (isLight) Color(0xFFFAFAFA).copy(alpha = 0.5f)
        else Color(0xFF121212).copy(alpha = 0.5f)
    val textColor =
        if (isLight) Color(0xCC111111) else Color(0xCCEEEEEE)

    val tabsBackdrop = rememberLayerBackdrop()
    val indicator = remember { Animatable(selectedIndex.toFloat()) }

    // 外部索引变化 → 胶囊平滑流动（spring 弹性）
    LaunchedEffect(selectedIndex) {
        snapshotFlow { selectedIndex }
            .drop(1)
            .collectLatest { i ->
                indicator.animateTo(
                    i.toFloat(),
                    spring(dampingRatio = 0.68f, stiffness = 380f),
                )
            }
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .height(64.dp),
    ) {
        val tabWidth = constraints.maxWidth / tabs.size

        // 0) 采样层：把背后内容录制进 tabsBackdrop（不绘制可见内容）
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .layerBackdrop(tabsBackdrop),
        )

        // 1) 玻璃面板：实时折射背后的内容
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .drawBackdrop(
                    backdrop = tabsBackdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(10.dp.toPx())
                        lens(22.dp.toPx(), 22.dp.toPx())
                    },
                    onDrawSurface = { drawRect(containerColor) },
                ),
        )

        // 2) 选中指示器：弹性胶囊，随 indicator 平移 + 按压鼓胀
        var pressed by remember { mutableStateOf(false) }
        val pressScale = remember { Animatable(1f) }
        LaunchedEffect(pressed) {
            if (pressed) {
                pressScale.animateTo(
                    1.08f,
                    spring(dampingRatio = 0.4f, stiffness = 500f),
                )
            } else {
                pressScale.animateTo(
                    1f,
                    spring(dampingRatio = 0.6f, stiffness = 400f),
                )
            }
        }
        Box(
            Modifier
                .graphicsLayer {
                    translationX = indicator.value * tabWidth
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                }
                .fillMaxWidth(1f / tabs.size)
                .height(64.dp)
                .padding(horizontal = 8.dp)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        lens(12.dp.toPx(), 14.dp.toPx(), chromaticAberration = true)
                    },
                    highlight = {
                        Highlight.Default.copy(
                            alpha = 0.6f + pressScale.value * 0.3f,
                        )
                    },
                    shadow = { Shadow(alpha = 0.45f) },
                    innerShadow = { InnerShadow(radius = 6.dp, alpha = 0.35f) },
                    onDrawSurface = {
                        drawRect(
                            // 选中时渐变更亮（按压鼓胀 → 折射增强）
                            accent.copy(alpha = 0.22f + pressScale.value * 0.08f),
                        )
                    },
                ),
        )

        // 3) 内容与点击层（动效图标）
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, tab ->
                val selected = i == selectedIndex
                // 图标颜色动画
                val iconColor by animateColorAsState(
                    targetValue = if (selected) accent else textColor.copy(alpha = 0.6f),
                    animationSpec = tween(300),
                    label = "iconColor",
                )
                // 选中图标弹性放大
                val iconScale = remember {
                    Animatable(if (selected) 1f else 0.92f)
                }
                LaunchedEffect(selected) {
                    iconScale.animateTo(
                        if (selected) 1f else 0.92f,
                        spring(dampingRatio = 0.5f, stiffness = 400f),
                    )
                }
                // 文字颜色动画
                val labelColor by animateColorAsState(
                    targetValue = if (selected) accent else textColor.copy(alpha = 0.6f),
                    animationSpec = tween(300),
                    label = "labelColor",
                )
                Column(
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!selected) {
                                pressed = true
                                onSelected(i)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (selected) tab.activeIcon else tab.icon,
                        contentDescription = tab.label,
                        tint = iconColor,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(iconScale.value),
                    )
                    Text(
                        text = tab.label,
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }

        // 松手释放按压状态（延迟避免闪烁）
        LaunchedEffect(selectedIndex) {
            kotlinx.coroutines.delay(150)
            pressed = false
        }
    }
}
package com.xumitech.tv.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * 原理：用 [layerBackdrop] 把导航栏区域背后的内容录制进 [tabsBackdrop] 图层，
 * 再在 [drawBackdrop] 中对该图层做 lens 折射 + blur 模糊 + vibrancy 增艳，
 * 实现"背后列表滚动时导航栏实时折射变形"的真液态效果。
 * 选中项是一枚弹性缩放的胶囊指示器（折射+高光+阴影），随切换平滑流动。
 *
 * 视觉对齐 AndroidLiquidGlass `LiquidBottomTabs`：
 * - 浮动胶囊容器：lens(22dp) + blur(10dp) + vibrancy，容器色半透明
 * - 选中指示器：lens 折射 + chromatic aberration + Highlight + Shadow + InnerShadow
 * - 切换动画：spring 弹性流动（dampingRatio 0.68）
 */
@Composable
fun LiquidNavBar(
    tabs: List<NavTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = !isSystemInDarkTheme()
    val accent = if (isLight) Color(0xFF0088FF) else Color(0xFF0091FF)
    val containerColor =
        if (isLight) Color(0xFFFAFAFA).copy(alpha = 0.5f)
        else Color(0xFF121212).copy(alpha = 0.5f)
    val textColor =
        if (isLight) Color(0xCC111111) else Color(0xCCEEEEEE)

    val tabsBackdrop = rememberLayerBackdrop()
    val indicator = remember { Animatable(selectedIndex.toFloat()) }

    // 外部索引变化 → 胶囊平滑流动
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

        // 2) 选中指示器：弹性胶囊，随 indicator 平移
        Box(
            Modifier
                .graphicsLayer {
                    translationX = indicator.value * tabWidth
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
                    highlight = { Highlight.Default.copy(alpha = 0.9f) },
                    shadow = { Shadow(alpha = 0.45f) },
                    innerShadow = { InnerShadow(radius = 6.dp, alpha = 0.35f) },
                    onDrawSurface = { drawRect(accent.copy(alpha = 0.26f)) },
                ),
        )

        // 3) 内容与点击层
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, tab ->
                val selected = i == selectedIndex
                Column(
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { if (!selected) onSelected(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (selected) tab.activeIcon else tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) accent else textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = tab.label,
                        color = if (selected) accent else textColor.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
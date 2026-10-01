package com.xumitech.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---------------- 品牌色（深色影视风 月光紫 + 天空蓝） ----------------
val Primary = Color(0xFF6C5CE7)        // 月光紫
val PrimaryDark = Color(0xFF5A4BD6)
val PrimaryLight = Color(0xFF8B7CF8)
val SkyBlue = Color(0xFF0EA5E9)        // 天空蓝
val Aqua = Color(0xFF22D3EE)           // 青蓝（渐变尾色）
val Secondary = Color(0xFFFF6B81)      // 珊瑚粉
val Gold = Color(0xFFFFC53D)           // 琥珀金（评分）
val Green = Color(0xFF2ED573)          // 翡翠绿（速度/更新）
val Danger = Color(0xFFFF5D6C)         // 危险红

// 液态玻璃导航栏 accent（对齐 AndroidLiquidGlass 官方蓝, 微调为品牌紫蓝渐变）
val LiquidAccentLight = Color(0xFF4F6BFF)   // 浅色: 电光紫蓝
val LiquidAccentDark = Color(0xFF7C8CFF)    // 深色: 亮紫蓝

// ---------------- 登录页渐变（紫 → 蓝 → 深底, 更通透） ----------------
val LoginGradientTop = Color(0xFF201650)
val LoginGradientMid = Color(0xFF3B2178)
val LoginGradientBottom = Color(0xFF0B0C14)

// ---------------- 深色色板（默认影视风, 品牌紫蓝层次增强） ----------------
private val DarkColors = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color(0xFF140F33),
    primaryContainer = Color(0xFF4A36B8),
    onPrimaryContainer = Color(0xFFEDE9FF),
    secondary = Color(0xFFFF8A9B),
    onSecondary = Color(0xFF3D0A14),
    secondaryContainer = Color(0xFF5A2330),
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = Gold,
    onTertiary = Color(0xFF3D2C00),
    tertiaryContainer = Color(0xFF5C4400),
    onTertiaryContainer = Color(0xFFFFDEA0),
    background = Color(0xFF0A0B12),
    onBackground = Color(0xFFECEDF3),
    surface = Color(0xFF131521),
    onSurface = Color(0xFFECEDF3),
    surfaceVariant = Color(0xFF1D2030),
    onSurfaceVariant = Color(0xFFA9ADC2),
    surfaceContainerLowest = Color(0xFF08090E),
    surfaceContainerLow = Color(0xFF0F1018),
    surfaceContainer = Color(0xFF131521),
    surfaceContainerHigh = Color(0xFF191B28),
    surfaceContainerHighest = Color(0xFF20232F),
    outline = Color(0xFF2A2D3A),
    outlineVariant = Color(0xFF3A3E4E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

// ---------------- 浅色色板（品牌紫蓝, 柔和渐变） ----------------
private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E3FF),
    onPrimaryContainer = Color(0xFF1A1060),
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF40121B),
    tertiary = Color(0xFF8A6D00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE3A8),
    onTertiaryContainer = Color(0xFF2B2000),
    background = Color(0xFFF6F7FC),
    onBackground = Color(0xFF1A1C24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C24),
    surfaceVariant = Color(0xFFEDEFF7),
    onSurfaceVariant = Color(0xFF4A4E5C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F8FD),
    surfaceContainer = Color(0xFFF1F2F9),
    surfaceContainerHigh = Color(0xFFEBECF4),
    surfaceContainerHighest = Color(0xFFE3E5EF),
    outline = Color(0xFFD6D9E4),
    outlineVariant = Color(0xFFC4C8D4),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

@Composable
fun XumiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
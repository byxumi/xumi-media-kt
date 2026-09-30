package com.xumitech.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 与须弥Media Flutter 版一致的强调色（深色影视风 月光紫 + 天空蓝点缀）
val Primary = Color(0xFF6C5CE7)
val PrimaryDark = Color(0xFF5A4BD6)
val PrimaryLight = Color(0xFF8B7CF8)
val SkyBlue = Color(0xFF0EA5E9)
val Secondary = Color(0xFFFF6B81)
val Gold = Color(0xFFFFC53D)
val Green = Color(0xFF2ED573)

// 液态玻璃导航栏配色（对齐 AndroidLiquidGlass accent）
val LiquidAccentLight = Color(0xFF0088FF)
val LiquidAccentDark = Color(0xFF0091FF)

private val LightColors = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Gold,
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF0F7),
    onSurface = Color(0xFF1A1C24),
    outline = Color(0xFFD6D9E4),
)

private val DarkColors = darkColorScheme(
    primary = PrimaryLight,
    secondary = Color(0xFFFF8A9B),
    tertiary = Gold,
    background = Color(0xFF0B0C12),
    surface = Color(0xFF14161F),
    surfaceVariant = Color(0xFF1E2130),
    onSurface = Color(0xFFECEDF3),
    outline = Color(0xFF2A2D3A),
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
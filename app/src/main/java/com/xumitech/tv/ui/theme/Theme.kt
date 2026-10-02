package com.xumitech.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ================= 品牌 Token(单一 accent:琥珀金 + 深蓝影院黑) =================

/** 交互/主色 —— 全应用唯一 accent。 */
val Accent = Color(0xFFE0A458)          // 琥珀金
val AccentDark = Color(0xFFC98A3E)
val AccentContainer = Color(0xFF3A2F1E)
val OnAccent = Color(0xFF1A140E)

/** 深色影院底。 */
val CinemaBlack = Color(0xFF08090C)
val CinemaSurface = Color(0xFF101218)
val CinemaSurfaceHi = Color(0xFF181B23)
val CinemaOutline = Color(0xFF262A33)
val CinemaOutlineHi = Color(0xFF333845)

/** 浅色模式底。 */
val Paper = Color(0xFFF5F3EE)
val PaperSurface = Color(0xFFFFFFFF)
val PaperOutline = Color(0xFFE3DFD6)

/** 语义色(非主 accent,仅功能性)。 */
val Success = Color(0xFF2ED573)
val Danger = Color(0xFFFF5D6C)
val Info = Color(0xFF4FC3F7)

/** 旧版品牌色(仅登录页氛围光晕使用,不进入交互 token)。 */
val BrandViolet = Color(0xFF6C5CE7)
val BrandSky = Color(0xFF0EA5E9)
val BrandAqua = Color(0xFF22D3EE)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentContainer,
    onPrimaryContainer = Color(0xFFFFE3B3),
    secondary = AccentDark,
    onSecondary = OnAccent,
    background = CinemaBlack,
    onBackground = Color(0xFFE9EAEE),
    surface = CinemaSurface,
    onSurface = Color(0xFFE9EAEE),
    surfaceVariant = CinemaSurfaceHi,
    onSurfaceVariant = Color(0xFF9AA0AC),
    surfaceContainerHighest = CinemaSurfaceHi,
    surfaceContainerHigh = CinemaSurfaceHi,
    surfaceContainer = CinemaSurface,
    surfaceContainerLow = CinemaSurface,
    surfaceContainerLowest = CinemaBlack,
    outline = CinemaOutline,
    outlineVariant = CinemaOutlineHi,
    error = Danger,
    onError = Color(0xFF2A0A0E),
    tertiary = Info,
    onTertiary = Color(0xFF06202B),
)

private val LightColors = lightColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF4E3C8),
    onPrimaryContainer = Color(0xFF3A2A12),
    secondary = AccentDark,
    onSecondary = Color(0xFFFFFFFF),
    background = Paper,
    onBackground = Color(0xFF1A1C20),
    surface = PaperSurface,
    onSurface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFFEDEAE3),
    onSurfaceVariant = Color(0xFF555A63),
    surfaceContainerHighest = Color(0xFFEDEAE3),
    surfaceContainerHigh = Color(0xFFF0EDE7),
    surfaceContainer = Color(0xFFF8F6F1),
    surfaceContainerLow = Color(0xFFFCFBF8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = PaperOutline,
    outlineVariant = Color(0xFFC9C4B8),
    error = Danger,
    onError = Color(0xFFFFFFFF),
    tertiary = Info,
    onTertiary = Color(0xFF06202B),
)

// ================= 形状 Token(收紧到 6 档) =================

/** xs:标签/小徽章 · sm:列表行 · md:输入框/卡片内侧 · lg:大卡/弹层 · xl:页面级容器。 */
val ShapeXs = RoundedCornerShape(6.dp)
val ShapeSm = RoundedCornerShape(8.dp)
val ShapeMd = RoundedCornerShape(12.dp)
val ShapeLg = RoundedCornerShape(16.dp)
val ShapeXl = RoundedCornerShape(24.dp)
val ShapeFull = RoundedCornerShape(50)

val XumiShapes = Shapes(
    extraSmall = ShapeXs,
    small = ShapeSm,
    medium = ShapeMd,
    large = ShapeLg,
    extraLarge = ShapeXl,
)

// ================= 字号 Token(收紧到 7 档) =================

val TypeHero = 32.sp
val TypeTitle = 22.sp
val TypeHeadline = 17.sp
val TypeBody = 14.sp
val TypeCaption = 12.sp
val TypeMicro = 11.sp
val TypeButton = 15.sp

val XumiTypography = Typography(
    displayLarge = TextStyle(fontSize = TypeHero, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = TypeTitle, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleLarge = TextStyle(fontSize = TypeTitle, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = TypeHeadline, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleSmall = TextStyle(fontSize = TypeBody, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = TypeBody, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = TypeBody, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = TypeCaption, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = TypeButton, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = TypeCaption, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = TypeMicro, fontWeight = FontWeight.Medium, lineHeight = 14.sp),
)

// ================= 主题入口 =================

@Composable
fun XumiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = XumiShapes,
        typography = XumiTypography,
        content = content,
    )
}

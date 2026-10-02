package com.xumitech.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xumitech.tv.BuildConfig
import com.xumitech.tv.ui.state.AppUiState
import com.xumitech.tv.ui.state.AppViewModel
import com.xumitech.tv.ui.state.AuthState
import com.xumitech.tv.ui.theme.Accent
import com.xumitech.tv.ui.theme.AccentDark
import com.xumitech.tv.ui.theme.ShapeLg

/** 我的页:用户卡 + 设置入口。 */
@Composable
fun ProfileScreen(
    vm: AppViewModel,
    ui: AppUiState,
    modifier: Modifier = Modifier,
    onOpenAdmin: () -> Unit,
) {
    var showLogout by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val username = (ui.auth as? AuthState.LoggedIn)?.username ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        // 用户卡
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ),
                    shape = ShapeLg,
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(Brush.linearGradient(listOf(Accent, AccentDark)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    username.take(1).ifBlank { "?" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    if ((ui.auth as? AuthState.LoggedIn)?.isAdmin == true) "管理员" else "普通用户",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 设置列表
        ProfileItem(
            icon = if (ui.themeMode == "light") Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
            title = "外观",
            subtitle = themeLabel(ui.themeMode),
            onClick = { cycleTheme(vm, ui.themeMode) },
        )
        ProfileItem(
            icon = Icons.Rounded.Info,
            title = "关于",
            subtitle = buildString {
                append("须弥Media v${BuildConfig.VERSION_NAME}")
                if (ui.serverVersion.isNotBlank()) append(" · 服务端 ${ui.serverVersion}")
            },
            onClick = { showAbout = true },
        )
        if ((ui.auth as? AuthState.LoggedIn)?.isAdmin == true) {
            ProfileItem(
                icon = Icons.Rounded.AdminPanelSettings,
                title = "管理后台",
                subtitle = "站点 / 数据源 / 用户 / 分类",
                onClick = onOpenAdmin,
            )
        }

        Spacer(Modifier.height(32.dp))
        TextButton(
            onClick = { showLogout = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Logout,
                null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.size(6.dp))
            Text("退出登录", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(onClick = { showLogout = false; vm.logout() }) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) { Text("取消") }
            },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("关于") },
            text = {
                Column {
                    Text("须弥Media · 聚合影视客户端")
                    Spacer(Modifier.height(8.dp))
                    Text("版本 v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (ui.siteName.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("站点 ${ui.siteName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("好") }
            },
        )
    }
}

@Composable
private fun ProfileItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text("›", color = MaterialTheme.colorScheme.outline, fontSize = 20.sp)
    }
}

private fun themeLabel(mode: String): String = when (mode) {
    "dark" -> "深色"
    "light" -> "浅色"
    else -> "跟随系统"
}

private fun cycleTheme(vm: AppViewModel, current: String) {
    vm.changeThemeMode(
        when (current) {
            "system" -> "dark"
            "dark" -> "light"
            else -> "system"
        },
    )
}

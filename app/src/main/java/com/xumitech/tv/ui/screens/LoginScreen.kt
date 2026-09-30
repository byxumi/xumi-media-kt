package com.xumitech.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xumitech.tv.AppState
import com.xumitech.tv.data.MoonTvApi
import com.xumitech.tv.ui.theme.Primary

/**
 * 登录/注册页：服务器地址 + 用户名 + 密码。
 * 视觉：渐变背景 + 玻璃感卡片 + 圆角输入框。
 */
@Composable
fun LoginScreen(
    appState: AppState,
    onLoggedIn: () -> Unit,
) {
    var server by remember { mutableStateOf(MoonTvApi.DEFAULT_SERVER) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0C12), Color(0xFF1A1440), Color(0xFF0B0C12)),
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(40.dp))

            // Logo 区
            Box(
                Modifier
                    .size(76.dp)
                    .background(
                        Brush.linearGradient(listOf(Primary, Color(0xFF0EA5E9))),
                        RoundedCornerShape(24.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("须", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "须弥Media",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "聚合搜索 · 优选测速 · 云端收藏续播",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(36.dp))

            // 输入卡片
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14161F).copy(alpha = 0.75f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                Text(
                    if (isRegister) "创建账号" else "欢迎回来",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("服务器地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Rounded.VisibilityOff
                                else Icons.Rounded.Visibility,
                                contentDescription = "显示密码",
                                tint = Color.White.copy(alpha = 0.5f),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )

                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                    )
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (server.isBlank() || username.isBlank() || password.isBlank()) {
                            error = "请填写完整信息"
                            return@Button
                        }
                        loading = true
                        error = null
                        if (isRegister) {
                            appState.register(server, username, password) { e ->
                                loading = false
                                if (e == null) onLoggedIn() else error = e
                            }
                        } else {
                            appState.login(server, username, password) { e ->
                                loading = false
                                if (e == null) onLoggedIn() else error = e
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(
                            if (isRegister) "注册并登录" else "登 录",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        isRegister = !isRegister
                        error = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        if (isRegister) "已有账号？去登录" else "没有账号？去注册",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
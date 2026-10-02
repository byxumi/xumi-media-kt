package com.xumitech.tv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xumitech.tv.ui.state.AppViewModel
import com.xumitech.tv.ui.theme.Accent
import com.xumitech.tv.ui.theme.BrandAqua
import com.xumitech.tv.ui.theme.BrandSky
import com.xumitech.tv.ui.theme.BrandViolet
import com.xumitech.tv.ui.theme.ShapeLg
import com.xumitech.tv.ui.theme.ShapeMd

/** 登录/注册页:深色影院氛围 + 品牌光晕 + 玻璃表单卡。 */
@Composable
fun LoginScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val server = remember { vm.ui.value.server }

    fun submit() {
        if (loading) return
        error = null
        when {
            username.isBlank() -> error = "请输入用户名"
            password.length < 6 -> error = "密码至少 6 位"
            isRegister && password != confirm -> error = "两次输入的密码不一致"
            else -> {
                loading = true
                val onDone: (Boolean, String?) -> Unit = { ok, msg ->
                    loading = false
                    if (!ok) error = msg ?: "操作失败"
                }
                if (isRegister) {
                    vm.register(username.trim(), password, onDone)
                } else {
                    vm.login(username.trim(), password, onDone)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0C12), Color(0xFF13101A), Color(0xFF08090C)))),
    ) {
        // 品牌氛围光晕
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(BrandViolet.copy(alpha = 0.35f), Color.Transparent)))
                .blur(40.dp),
        )
        Box(
            Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 60.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(BrandSky.copy(alpha = 0.28f), Color.Transparent)))
                .blur(50.dp),
        )
        Box(
            Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 30.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(BrandAqua.copy(alpha = 0.20f), Color.Transparent)))
                .blur(40.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(48.dp))
            // Logo
            Box(
                Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(BrandViolet, BrandSky, BrandAqua))),
                contentAlignment = Alignment.Center,
            ) {
                Text("须", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                server,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "聚合影视 · 一搜即看",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))

            // 表单卡
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeLg)
                    .background(
                        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                            Color(0xFF14161F).copy(alpha = 0.86f)
                        } else {
                            Color.White.copy(alpha = 0.9f)
                        },
                    )
                    .padding(20.dp),
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("用户名") },
                    singleLine = true,
                    shape = ShapeMd,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = ShapeMd,
                )
                AnimatedVisibility(isRegister, enter = fadeIn(), exit = fadeOut()) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("确认密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = ShapeMd,
                        )
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { submit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !loading,
                    shape = ShapeMd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            if (isRegister) "注册并进入" else "登 录",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { isRegister = !isRegister; error = null },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isRegister) "已有账号？去登录" else "没有账号？去注册")
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "v3.0 · Dark Cinema",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

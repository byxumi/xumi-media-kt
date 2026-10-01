package com.xumitech.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoNotDisturb
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xumitech.tv.data.MoonTvApi
import com.xumitech.tv.ui.theme.Aqua
import com.xumitech.tv.ui.theme.Danger
import com.xumitech.tv.ui.theme.Green
import com.xumitech.tv.ui.theme.Primary
import com.xumitech.tv.ui.theme.SkyBlue
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 管理后台（仅 owner/admin 可见）。
 * 分区:
 * - 站点设置: 站名/公告/注册开关/进度保存间隔/TVBox
 * - 数据源: 列表 + 添加/删除/启用/禁用
 * - 用户: 列表 + 封禁/解封/设管理员/删除/改密
 * - 自定义分类: 列表 + 添加/删除
 * 数据来自 GET /api/admin/config, 写入走 POST /api/admin/{site,source,category,user}。
 */
@Composable
fun AdminScreen(
    onClose: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var adminJson by remember { mutableStateOf<JsonObject?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try {
            loading = true
            error = null
            adminJson = MoonTvApi.getAdminConfig()
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            // 顶栏
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    "管理后台",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { scope.launch { reload() } }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                }
            }

            // Tab 切换
            val tabs = listOf("站点", "数据源", "用户", "分类")
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabs.forEachIndexed { i, t ->
                    val selected = i == tab
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) Brush.linearGradient(listOf(Primary, SkyBlue))
                                else Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                ),
                            )
                            .clickable { tab = i }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            t,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { scope.launch { reload() } }) {
                            Text("点击重试", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                adminJson != null -> when (tab) {
                    0 -> SiteTab(adminJson!!, onSaved = { scope.launch { reload() } })
                    1 -> SourceTab(adminJson!!, onChanged = { scope.launch { reload() } })
                    2 -> UserTab(adminJson!!, onChanged = { scope.launch { reload() } })
                    else -> CategoryTab(adminJson!!, onChanged = { scope.launch { reload() } })
                }
            }
        }
    }
}

// ---------------- 工具 ----------------

/** 统一的状态提示文本：去掉 ✅/❌ 前缀，按成败着色。 */
@Composable
private fun StatusMsg(text: String?, modifier: Modifier = Modifier) {
    if (text == null) return
    val ok = !text.startsWith("❌")
    Text(
        text.removePrefix("✅").removePrefix("❌").trim(),
        color = if (ok) Green else Danger,
        fontSize = 12.sp,
        modifier = modifier,
    )
}

/** 危险操作确认对话框。 */
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "删除",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onConfirm()
                },
            ) { Text(confirmLabel, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun JsonObject.config() = get("Config") as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.site() = config().get("SiteConfig") as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.userCfg() = config().get("UserConfig") as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.objList(key: String): List<JsonObject> =
    (get(key) as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
private fun JsonObject.s(key: String): String = (this[key]?.jsonPrimitive?.contentOrNull ?: "")
private fun JsonObject.b(key: String): Boolean =
    (this[key]?.jsonPrimitive?.contentOrNull ?: "false").toBooleanStrictOrNull() ?: false

@Composable
private fun AdminCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) { content() }
}

@Composable
private fun AdminSectionTitle(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

// ---------------- 站点设置 ----------------
@Composable
private fun SiteTab(json: JsonObject, onSaved: () -> Unit) {
    val site = json.site()
    var siteName by remember(site) { mutableStateOf(site.s("SiteName")) }
    var announcement by remember(site) { mutableStateOf(site.s("Announcement")) }
    var allowRegister by remember(json) { mutableStateOf(json.userCfg().b("AllowRegister")) }
    var saveInterval by remember(site) { mutableStateOf(site.s("PlaybackSaveInterval").ifEmpty { "5" }) }
    var tvbox by remember(site) { mutableStateOf(site.b("TVBoxEnabled")) }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.verticalScroll(rememberScrollState())) {
        AdminSectionTitle("站点信息")
        AdminCard {
            OutlinedTextField(
                value = siteName,
                onValueChange = { siteName = it },
                label = { Text("站点名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = announcement,
                onValueChange = { announcement = it },
                label = { Text("公告") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("允许注册", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(checked = allowRegister, onCheckedChange = { allowRegister = it })
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TVBox 接口", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(checked = tvbox, onCheckedChange = { tvbox = it })
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = saveInterval,
                onValueChange = { saveInterval = it.filter(Char::isDigit) },
                label = { Text("进度保存间隔(秒)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            if (msg != null) {
                Spacer(Modifier.height(8.dp))
                StatusMsg(msg)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Primary, SkyBlue, Aqua)))
                    .clickable(enabled = !saving) {
                        scope.launch {
                            saving = true
                            msg = null
                            try {
                                MoonTvApi.saveAdminSite(
                                    mapOf(
                                        "SiteName" to siteName,
                                        "Announcement" to announcement,
                                        "DisableYellowFilter" to site.b("DisableYellowFilter"),
                                        "PlaybackSaveInterval" to (saveInterval.toIntOrNull() ?: 5),
                                        "TVBoxEnabled" to tvbox,
                                        "TVBoxPassword" to site.s("TVBoxPassword"),
                                    ),
                                )
                                MoonTvApi.adminUser("setAllowRegister", allowRegister = allowRegister)
                                msg = "✅ 已保存"
                                onSaved()
                            } catch (e: Exception) {
                                msg = "❌ ${e.message}"
                            } finally {
                                saving = false
                            }
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (saving) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("保存设置", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------- 数据源 ----------------
@Composable
private fun SourceTab(json: JsonObject, onChanged: () -> Unit) {
    val sources = remember(json) { json.config().objList("SourceConfig") }
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<JsonObject?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("共 ${sources.size} 个数据源", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { showAdd = true }) {
                Text("+ 添加", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        if (msg != null) {
            StatusMsg(msg, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
        LazyColumn {
            items(sources, key = { it.s("key") }) { src ->
                val disabled = src.b("disabled")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = if (disabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            src.s("name").ifEmpty { src.s("key") },
                            color = if (disabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            src.s("api"),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                    if (disabled) {
                        Text("已停用", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp)
                    } else {
                        Text("启用", color = Green, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                MoonTvApi.adminSource(if (disabled) "enable" else "disable", key = src.s("key"))
                                msg = if (disabled) "✅ 已启用 ${src.s("name")}" else "✅ 已停用 ${src.s("name")}"
                                onChanged()
                            } catch (e: Exception) { msg = "❌ ${e.message}" }
                        }
                    }) {
                        Icon(
                            if (disabled) Icons.Rounded.CheckCircle else Icons.Rounded.DoNotDisturb,
                            contentDescription = if (disabled) "启用" else "停用",
                            tint = if (disabled) Green else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = { pendingDelete = src }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = Danger, modifier = Modifier.size(18.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAdd) {
        AddSourceDialog(
            onConfirm = { key, name, api, detail ->
                scope.launch {
                    try {
                        MoonTvApi.adminSource("add", key = key, name = name, api = api, detail = detail)
                        msg = "✅ 已添加 $name"
                        showAdd = false
                        onChanged()
                    } catch (e: Exception) { msg = "❌ ${e.message}" }
                }
            },
            onDismiss = { showAdd = false },
        )
    }
    pendingDelete?.let { src ->
        ConfirmDialog(
            title = "删除数据源",
            message = "确定删除「${src.s("name").ifEmpty { src.s("key") }}」？该操作不可恢复。",
            onConfirm = {
                scope.launch {
                    try {
                        MoonTvApi.adminSource("delete", key = src.s("key"))
                        msg = "✅ 已删除 ${src.s("name")}"
                        onChanged()
                    } catch (e: Exception) { msg = "❌ ${e.message}" }
                }
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun AddSourceDialog(
    onConfirm: (key: String, name: String, api: String, detail: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var api by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加数据源") },
        text = {
            Column {
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("key(唯一标识)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = api, onValueChange = { api = it }, label = { Text("API 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = detail, onValueChange = { detail = it }, label = { Text("详情地址(可选)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    key.isBlank() -> error = "请填写 key（唯一标识）"
                    name.isBlank() -> error = "请填写名称"
                    api.isBlank() -> error = "请填写 API 地址"
                    else -> {
                        error = null
                        onConfirm(key.trim(), name.trim(), api.trim(), detail.trim())
                    }
                }
            }) { Text("添加", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

// ---------------- 用户管理 ----------------
@Composable
private fun UserTab(json: JsonObject, onChanged: () -> Unit) {
    val users = remember(json) { json.userCfg().objList("Users") }
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<JsonObject?>(null) }

    Column {
        AdminSectionTitle("用户列表(${users.size})")
        if (msg != null) {
            StatusMsg(msg, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
        LazyColumn {
            items(users, key = { it.s("username") }) { u ->
                val banned = u.b("banned")
                val role = u.s("role")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = if (role == "owner") MaterialTheme.colorScheme.tertiary
                        else if (role == "admin") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            u.s("username"),
                            color = if (banned) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            when (role) {
                                "owner" -> "站长"
                                "admin" -> "管理员"
                                else -> "普通用户"
                            } + if (banned) " · 已封禁" else "",
                            color = if (banned) Danger else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                        )
                    }
                    if (role != "owner") {
                        if (role == "admin") {
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        MoonTvApi.adminUser("cancelAdmin", targetUsername = u.s("username"))
                                        msg = "✅ 已取消 ${u.s("username")} 管理员"
                                        onChanged()
                                    } catch (e: Exception) { msg = "❌ ${e.message}" }
                                }
                            }) { Text("取消管理", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp) }
                        } else {
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        MoonTvApi.adminUser("setAdmin", targetUsername = u.s("username"))
                                        msg = "✅ 已设为管理员 ${u.s("username")}"
                                        onChanged()
                                    } catch (e: Exception) { msg = "❌ ${e.message}" }
                                }
                            }) { Text("设为管理", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
                        }
                        if (banned) {
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        MoonTvApi.adminUser("unban", targetUsername = u.s("username"))
                                        msg = "✅ 已解封 ${u.s("username")}"
                                        onChanged()
                                    } catch (e: Exception) { msg = "❌ ${e.message}" }
                                }
                            }) { Icon(Icons.Rounded.CheckCircle, contentDescription = "解封", tint = Green, modifier = Modifier.size(18.dp)) }
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        MoonTvApi.adminUser("ban", targetUsername = u.s("username"))
                                        msg = "✅ 已封禁 ${u.s("username")}"
                                        onChanged()
                                    } catch (e: Exception) { msg = "❌ ${e.message}" }
                                }
                            }) { Icon(Icons.Rounded.Block, contentDescription = "封禁", tint = Danger, modifier = Modifier.size(18.dp)) }
                        }
                        IconButton(onClick = { pendingDelete = u }) { Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = Danger, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    pendingDelete?.let { u ->
        ConfirmDialog(
            title = "删除用户",
            message = "确定删除用户「${u.s("username")}」？该操作不可恢复。",
            onConfirm = {
                scope.launch {
                    try {
                        MoonTvApi.adminUser("deleteUser", targetUsername = u.s("username"))
                        msg = "✅ 已删除用户 ${u.s("username")}"
                        onChanged()
                    } catch (e: Exception) { msg = "❌ ${e.message}" }
                }
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

// ---------------- 自定义分类 ----------------
@Composable
private fun CategoryTab(json: JsonObject, onChanged: () -> Unit) {
    val cats = remember(json) { json.config().objList("CustomCategories") }
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<JsonObject?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("共 ${cats.size} 个自定义分类", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { showAdd = true }) { Text("+ 添加", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }
        if (msg != null) {
            StatusMsg(msg, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
        LazyColumn {
            items(cats, key = { it.s("query") + it.s("name") }) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            c.s("name").ifEmpty { "未命名" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "${c.s("type")} · ${c.s("query")}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { pendingDelete = c }) { Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = Danger, modifier = Modifier.size(18.dp)) }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加自定义分类") },
            text = {
                var name by remember { mutableStateOf("") }
                var type by remember { mutableStateOf("movie") }
                var query by remember { mutableStateOf("") }
                var error by remember { mutableStateOf<String?>(null) }
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("分类名") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.height(6.dp))
                    Row {
                        listOf("movie", "tv").forEach { t ->
                            val sel = type == t
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { type = t }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (t == "movie") "电影" else "剧集", color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (t == "movie") Spacer(Modifier.width(8.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("搜索关键词") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            when {
                                name.isBlank() -> error = "请填写分类名"
                                query.isBlank() -> error = "请填写搜索关键词"
                                else -> {
                                    error = null
                                    scope.launch {
                                        try {
                                            MoonTvApi.adminCategory("add", name = name.trim(), type = type, query = query.trim())
                                            msg = "✅ 已添加 $name"
                                            showAdd = false
                                            onChanged()
                                        } catch (e: Exception) { msg = "❌ ${e.message}" }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("添加", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } },
        )
    }

    pendingDelete?.let { c ->
        ConfirmDialog(
            title = "删除分类",
            message = "确定删除分类「${c.s("name").ifEmpty { "未命名" }}」？该操作不可恢复。",
            onConfirm = {
                scope.launch {
                    try {
                        MoonTvApi.adminCategory("delete", name = c.s("name"))
                        msg = "✅ 已删除 ${c.s("name")}"
                        onChanged()
                    } catch (e: Exception) { msg = "❌ ${e.message}" }
                }
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
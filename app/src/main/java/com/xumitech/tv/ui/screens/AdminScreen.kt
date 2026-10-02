package com.xumitech.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xumitech.tv.data.MoonApi
import com.xumitech.tv.ui.components.SimpleError
import com.xumitech.tv.ui.components.SimpleEmpty
import com.xumitech.tv.ui.theme.Accent
import com.xumitech.tv.ui.theme.Danger
import com.xumitech.tv.ui.theme.Success
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** 管理后台(仅 owner/admin 可见)。站点/数据源/用户/分类 四 Tab。 */
@Composable
fun AdminScreen(onClose: () -> Unit) {
    var adminJson by remember { mutableStateOf<JsonObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                adminJson = MoonApi.getAdminConfig()
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    fun adminRun(action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
                msg = "操作成功"
                reload()
            } catch (e: Exception) {
                msg = e.message ?: "操作失败"
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 顶部栏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Text(
                "管理后台",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { reload() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
            }
        }

        msg?.let {
            Text(
                it,
                color = if (it.startsWith("操作成功")) Success else Danger,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        TabRow(selectedTabIndex = tab) {
            listOf("站点", "数据源", "用户", "分类").forEachIndexed { i, t ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text(t, fontSize = 14.sp) },
                )
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> SimpleError(error ?: "加载失败", onRetry = { reload() })
            adminJson == null -> SimpleEmpty("暂无数据", actionLabel = "刷新", onAction = { reload() })
            else -> when (tab) {
                0 -> SiteTab(adminJson!!, onSaved = { msg = "操作成功" }, scope = scope)
                1 -> SourceTab(adminJson!!) { adminRun(it) }
                2 -> UserTab(adminJson!!) { adminRun(it) }
                else -> CategoryTab(adminJson!!) { adminRun(it) }
            }
        }
    }
}

// ---------------- JSON 解析辅助 ----------------
private fun JsonObject.config() = get("Config") as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.site() = config().get("SiteConfig") as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.userCfg() = config().get("UserConfig") as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.objList(key: String): List<JsonObject> =
    (get(key) as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
private fun JsonObject.s(key: String): String = this[key]?.jsonPrimitive?.contentOrNull ?: ""
private fun JsonObject.b(key: String): Boolean = this[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false

// ---------------- 通用卡片/表单 ----------------
@Composable
private fun AdminCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------------- 站点设置 ----------------
@Composable
private fun SiteTab(json: JsonObject, onSaved: (String) -> Unit, scope: kotlinx.coroutines.CoroutineScope) {
    val site = json.site()
    var siteName by remember(site) { mutableStateOf(site.s("SiteName")) }
    var announcement by remember(site) { mutableStateOf(site.s("Announcement")) }
    var allowRegister by remember(json) { mutableStateOf(json.userCfg().b("AllowRegister")) }
    var saveInterval by remember(site) { mutableStateOf(site.s("PlaybackSaveInterval").ifEmpty { "5" }) }
    var tvbox by remember(site) { mutableStateOf(site.b("TVBoxEnabled")) }
    var saving by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        AdminCard {
            FieldLabel("站点名称")
            OutlinedTextField(
                value = siteName,
                onValueChange = { siteName = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            FieldLabel("公告")
            OutlinedTextField(
                value = announcement,
                onValueChange = { announcement = it },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("允许注册", Modifier.weight(1f), fontSize = 14.sp)
                Switch(checked = allowRegister, onCheckedChange = { allowRegister = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TVBox 接口", Modifier.weight(1f), fontSize = 14.sp)
                Switch(checked = tvbox, onCheckedChange = { tvbox = it })
            }
            FieldLabel("进度保存间隔(分钟)")
            OutlinedTextField(
                value = saveInterval,
                onValueChange = { saveInterval = it.filter(Char::isDigit) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            TextButton(
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            MoonApi.saveAdminSite(
                                mapOf(
                                    "SiteName" to siteName,
                                    "Announcement" to announcement,
                                    "PlaybackSaveInterval" to (saveInterval.toIntOrNull() ?: 5),
                                    "TVBoxEnabled" to tvbox,
                                ),
                            )
                            MoonApi.adminUser("setAllowRegister", allowRegister = allowRegister)
                            onSaved("操作成功")
                        } catch (e: Exception) {
                            onSaved(e.message ?: "保存失败")
                        }
                        saving = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
            ) {
                Text(if (saving) "保存中…" else "保存设置", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------- 数据源 ----------------
@Composable
private fun SourceTab(json: JsonObject, adminRun: (suspend () -> Unit) -> Unit) {
    val sources = remember(json) { json.config().objList("SourceConfig") }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<JsonObject?>(null) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (sources.isEmpty()) {
                item { SimpleEmpty("还没有数据源") }
            }
            items(sources, key = { it.s("key") }) { src ->
                val name = src.s("name").ifEmpty { src.s("key") }
                val disabled = src.b("disabled")
                SurfaceRow {
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (src.s("api").isNotEmpty()) {
                            Text(src.s("api"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    IconButton(onClick = { adminRun { MoonApi.adminSource(if (disabled) "enable" else "disable", key = src.s("key")) } }) {
                        Icon(
                            if (disabled) Icons.Rounded.VideoLibrary else Icons.Rounded.Block,
                            contentDescription = if (disabled) "启用" else "停用",
                            tint = if (disabled) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { pendingDelete = src }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = Danger)
                    }
                }
            }
            item {
                TextButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ 添加数据源", fontWeight = FontWeight.Bold, color = Accent)
                }
            }
        }
    }

    if (showAdd) {
        var key by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var api by remember { mutableStateOf("") }
        var detail by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加数据源") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("key(唯一标识)") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = api, onValueChange = { api = it }, label = { Text("API 地址") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = detail, onValueChange = { detail = it }, label = { Text("详情地址") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAdd = false
                    adminRun { MoonApi.adminSource("add", key = key.trim(), name = name.trim(), api = api.trim(), detail = detail.trim()) }
                }) { Text("添加", color = Accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } },
        )
    }

    pendingDelete?.let { src ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除数据源") },
            text = { Text("确定删除「${src.s("name").ifEmpty { src.s("key") }}」？该操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    val key = src.s("key")
                    pendingDelete = null
                    adminRun { MoonApi.adminSource("delete", key = key) }
                }) { Text("删除", color = Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

// ---------------- 用户 ----------------
@Composable
private fun UserTab(json: JsonObject, adminRun: (suspend () -> Unit) -> Unit) {
    val users = remember(json) { json.userCfg().objList("Users") }
    var pendingDelete by remember { mutableStateOf<JsonObject?>(null) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (users.isEmpty()) {
                item { SimpleEmpty("还没有用户") }
            }
            items(users, key = { it.s("username") }) { u ->
                val name = u.s("username")
                val role = u.s("role")
                val banned = u.b("banned")
                SurfaceRow {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = Accent)
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            when {
                                banned -> "已封禁"
                                role == "owner" -> "拥有者"
                                role == "admin" -> "管理员"
                                else -> "普通用户"
                            },
                            fontSize = 12.sp,
                            color = if (banned) Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (role != "owner") {
                        IconButton(onClick = { adminRun { MoonApi.adminUser(if (role == "admin") "cancelAdmin" else "setAdmin", targetUsername = name) } }) {
                            Icon(Icons.Rounded.Edit, contentDescription = if (role == "admin") "取消管理员" else "设为管理员", tint = if (role == "admin") Accent else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { adminRun { MoonApi.adminUser(if (banned) "unban" else "ban", targetUsername = name) } }) {
                            Icon(Icons.Rounded.Block, contentDescription = if (banned) "解封" else "封禁", tint = if (banned) Success else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { pendingDelete = u }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = Danger)
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { u ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除用户") },
            text = { Text("确定删除用户「${u.s("username")}」？该操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    val name = u.s("username")
                    pendingDelete = null
                    adminRun { MoonApi.adminUser("deleteUser", targetUsername = name) }
                }) { Text("删除", color = Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

// ---------------- 分类 ----------------
@Composable
private fun CategoryTab(json: JsonObject, adminRun: (suspend () -> Unit) -> Unit) {
    val cats = remember(json) { json.config().objList("CustomCategories") }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<JsonObject?>(null) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (cats.isEmpty()) {
                item { SimpleEmpty("还没有自定义分类") }
            }
            items(cats, key = { it.s("query") + it.s("name") }) { c ->
                SurfaceRow {
                    Icon(Icons.Rounded.Storage, contentDescription = null, tint = Accent)
                    Column(Modifier.weight(1f)) {
                        Text(c.s("name").ifEmpty { "未命名" }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${c.s("type")} · ${c.s("query")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { pendingDelete = c }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = Danger)
                    }
                }
            }
            item {
                TextButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ 添加分类", fontWeight = FontWeight.Bold, color = Accent)
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("movie") }
        var query by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加分类") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("类型(movie/tv/anime)") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("查询词") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAdd = false
                    adminRun { MoonApi.adminCategory("add", name = name.trim(), type = type.trim(), query = query.trim()) }
                }) { Text("添加", color = Accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } },
        )
    }

    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除分类") },
            text = { Text("确定删除分类「${c.s("name").ifEmpty { "未命名" }}」？该操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    val name = c.s("name")
                    pendingDelete = null
                    adminRun { MoonApi.adminCategory("delete", name = name) }
                }) { Text("删除", color = Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

// ---------------- 通用行 -----------------
@Composable
private fun SurfaceRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

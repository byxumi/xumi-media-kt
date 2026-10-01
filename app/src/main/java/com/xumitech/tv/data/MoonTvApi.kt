package com.xumitech.tv.data

import com.xumitech.tv.model.DoubanItem
import com.xumitech.tv.model.Favorite
import com.xumitech.tv.model.Following
import com.xumitech.tv.model.PlayRecord
import com.xumitech.tv.model.SearchResult
import com.xumitech.tv.model.ServerConfig
import com.xumitech.tv.model.Source
import com.xumitech.tv.model.TodayUpdatedRecord
import com.xumitech.tv.model.json as xumiJson
import com.xumitech.tv.model.str
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, message: String) : Exception(message)

/** MoonTV 后端客户端（cookie 鉴权）。 */
object MoonTvApi {
    const val DEFAULT_SERVER = "https://tv.xumitech.top"
    const val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private const val JSON_MEDIA = "application/json; charset=utf-8"

    @Volatile private var server: String = DEFAULT_SERVER
    @Volatile private var authCookie: String? = null

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    val baseUrl: String get() = server
    val isLoggedIn: Boolean get() = !authCookie.isNullOrEmpty()
    var onAuthChanged: (() -> Unit)? = null

    fun configure(newServer: String, cookie: String?) {
        server = newServer.trim().trimEnd('/').ifEmpty { DEFAULT_SERVER }
        authCookie = cookie?.takeIf { it.isNotBlank() }
        onAuthChanged?.invoke()
    }

    /** 从 auth cookie（URL 编码 JSON）解析角色：owner/admin -> 管理员。 */
    fun parseRole(): String {
        val c = authCookie ?: return ""
        return try {
            val decoded = decodeUriComponent(decodeUriComponent(c))
            val j = Json { ignoreUnknownKeys = true }
                .parseToJsonElement(decoded).jsonObject
            j["role"]?.let { it.toString().trim('"') } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun decodeUriComponent(s: String): String =
        java.net.URLDecoder.decode(s, "UTF-8")

    val isAdmin: Boolean get() {
        val r = parseRole()
        return r == "owner" || r == "admin"
    }

    private fun headers(withCookie: Boolean = true): Map<String, String> {
        val h = mutableMapOf(
            "User-Agent" to UA,
            "Accept" to "application/json",
        )
        if (withCookie) authCookie?.let { h["Cookie"] = "auth=$it" }
        return h
    }

    /**
     * 统一请求执行（强制 IO 线程，避免主线程 NetworkOnMainThreadException）。
     * 所有公开方法均为 suspend，调用方需在协程内调用。
     */
    private suspend fun <T> call(
        method: String,
        path: String,
        withCookie: Boolean = true,
        query: Map<String, String> = emptyMap(),
        body: Map<String, Any?>? = null,
        block: (Response) -> T,
    ): T = withContext(Dispatchers.IO) {
        val ub = (server + path).toHttpUrl().newBuilder()
        query.forEach { (k, v) -> ub.addQueryParameter(k, v) }
        val rb = Request.Builder().url(ub.build())
        headers(withCookie).forEach { (k, v) -> rb.header(k, v) }
        when (method.uppercase()) {
            "GET" -> rb.get()
            "POST" -> rb.post(body?.let { serializeBody(it) } ?: "{}".toRequestBody(JSON_MEDIA.toMediaType()))
            "DELETE" -> rb.delete(body?.let { serializeBody(it) } ?: "{}".toRequestBody(JSON_MEDIA.toMediaType()))
            else -> rb.get()
        }
        client.newCall(rb.build()).execute().use { res ->
            if (!res.isSuccessful) {
                throw ApiException(res.code, res.body?.string() ?: "HTTP ${res.code}")
            }
            // 捕获 auth set-cookie（登录后写回）
            res.headers("Set-Cookie").forEach { raw ->
                val eq = raw.indexOf('=')
                if (eq > 0 && raw.substring(0, eq) == "auth") {
                    val v = raw.substring(eq + 1).substringBefore(';').trim()
                    if (v.isNotEmpty()) {
                        authCookie = v
                        onAuthChanged?.invoke()
                    }
                }
            }
            block(res)
        }
    }

    /**
     * 序列化请求体为 JSON 字符串。
     * ⚠️ 修复旧版 bug：`joinToString` 参数顺序应为 (separator, prefix, postfix)，
     * 旧代码写成了 ("{", "}", ",") 导致 JSON 形如 `{"a":1{"b":2}` —— Kotlin 版无法登录的根因。
     */
    private fun serializeBody(m: Map<String, Any?>): okhttp3.RequestBody {
        val s = m.entries.joinToString(",", "{", "}") { (k, v) ->
            val enc = when (v) {
                is String -> "\"${v.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                null -> "null"
                is Map<*, *> -> serializeMap(v)
                is List<*> -> serializeList(v)
                else -> v.toString()
            }
            "\"$k\":$enc"
        }
        return s.toRequestBody(JSON_MEDIA.toMediaType())
    }

    private fun serializeMap(m: Map<*, *>): String =
        m.entries.joinToString(",", "{", "}") { (k, v) ->
            val key = k.toString().replace("\\", "\\\\").replace("\"", "\\\"")
            val enc = when (v) {
                is String -> "\"${v.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                null -> "null"
                is Map<*, *> -> serializeMap(v)
                is List<*> -> serializeList(v)
                else -> v.toString()
            }
            "\"$key\":$enc"
        }

    private fun serializeList(l: List<*>): String =
        l.joinToString(",", "[", "]") { v ->
            when (v) {
                is String -> "\"${v.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                null -> "null"
                is Map<*, *> -> serializeMap(v)
                is List<*> -> serializeList(v)
                else -> v.toString()
            }
        }

    private fun parseObject(res: Response): JsonObject {
        val text = res.body?.string() ?: "{}"
        return try {
            (xumiJson.parseToJsonElement(text) as? JsonObject) ?: JsonObject(emptyMap())
        } catch (_: Exception) {
            JsonObject(emptyMap())
        }
    }

    private fun parseList(res: Response): List<JsonObject> {
        val text = res.body?.string() ?: "[]"
        val arr = try {
            xumiJson.parseToJsonElement(text) as? JsonArray ?: JsonArray(emptyList())
        } catch (_: Exception) {
            JsonArray(emptyList())
        }
        return arr.mapNotNull { it as? JsonObject }
    }

    private fun parseStringList(res: Response): List<String> {
        val text = res.body?.string() ?: "[]"
        val arr = try {
            xumiJson.parseToJsonElement(text) as? JsonArray ?: JsonArray(emptyList())
        } catch (_: Exception) {
            JsonArray(emptyList())
        }
        return arr.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
    }

    // ---------------- 认证 ----------------
    suspend fun login(username: String, password: String): String {
        call("POST", "/api/login", withCookie = false,
            body = mapOf("username" to username, "password" to password)) { res ->
            res.code // set-cookie 已在 call 中捕获
        }
        return authCookie ?: ""
    }

    suspend fun register(username: String, password: String): String {
        call("POST", "/api/register", withCookie = false,
            body = mapOf("username" to username, "password" to password)) { res ->
            res.code
        }
        return authCookie ?: ""
    }

    suspend fun logout() {
        try {
            call("POST", "/api/logout") { res -> res.code }
        } catch (_: Exception) {}
        authCookie = null
        onAuthChanged?.invoke()
    }

    // ---------------- 基础数据 ----------------
    suspend fun getServerConfig(): ServerConfig {
        return call("GET", "/api/server-config", withCookie = false) { res ->
            ServerConfig.fromJson(parseObject(res))
        }
    }

    suspend fun getSources(): List<Source> {
        return call("GET", "/api/config/sources") { res ->
            parseList(res).map { Source.fromJson(it) }
        }
    }

    suspend fun detail(id: String, source: String): SearchResult {
        return call("GET", "/api/detail", query = mapOf("id" to id, "source" to source)) { res ->
            SearchResult.fromJson(parseObject(res))
        }
    }

    suspend fun searchVideo(query: String, stream: String = "0"): List<SearchResult> {
        return call("GET", "/api/search", query = mapOf("q" to query, "stream" to stream)) { res ->
            val j = parseObject(res)
            (j["results"] as? JsonArray)?.mapNotNull {
                (it as? JsonObject)?.let(SearchResult::fromJson)
            } ?: emptyList()
        }
    }

    /** 搜索建议（学 LunaTV /api/search/suggestions）。失败静默返回空。 */
    suspend fun searchSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return try {
            call("GET", "/api/search/suggestions", query = mapOf("q" to query)) { res ->
                val j = parseObject(res)
                (j["suggestions"] as? JsonArray)?.mapNotNull {
                    (it as? JsonObject)?.let { o -> o.str("text") }
                }?.filter { it.isNotEmpty() } ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 修改密码（学 LunaTV /api/change-password）。 */
    suspend fun changePassword(oldPassword: String, newPassword: String) {
        call("POST", "/api/change-password",
            body = mapOf("oldPassword" to oldPassword, "newPassword" to newPassword)) { res ->
            res.code
        }
    }

    suspend fun getDoubanRecommends(kind: String, limit: Int = 20, start: Int = 0): List<DoubanItem> {
        return call("GET", "/api/douban/recommends",
            query = mapOf("kind" to kind, "limit" to "$limit", "start" to "$start")) { res ->
            val j = parseObject(res)
            (j["list"] as? JsonArray)?.mapNotNull {
                (it as? JsonObject)?.let(DoubanItem::fromJson)
            } ?: emptyList()
        }
    }

    suspend fun getDoubanCategories(
        kind: String,
        category: String = "",
        type: String = "",
        limit: Int = 16,
        start: Int = 0,
    ): List<DoubanItem> {
        val q = mutableMapOf("kind" to kind, "limit" to "$limit", "start" to "$start")
        if (category.isNotEmpty()) q["category"] = category
        if (type.isNotEmpty()) q["type"] = type
        return call("GET", "/api/douban/categories", query = q) { res ->
            val j = parseObject(res)
            (j["list"] as? JsonArray)?.mapNotNull {
                (it as? JsonObject)?.let(DoubanItem::fromJson)
            } ?: emptyList()
        }
    }

    // ---------------- 追更（学网页端 Following） ----------------
    suspend fun getFollowings(): Map<String, Following> {
        return call("GET", "/api/followings") { res ->
            val j = parseObject(res)
            val out = mutableMapOf<String, Following>()
            j.forEach { (k, v) ->
                (v as? JsonObject)?.let { out[k] = Following.fromJson(it) }
            }
            out
        }
    }

    suspend fun saveFollowing(key: String, f: Following) {
        call("POST", "/api/followings",
            body = mapOf("key" to key, "following" to f.toJsonMap())) { res -> res.code }
    }

    suspend fun deleteFollowing(key: String) {
        call("DELETE", "/api/followings", query = mapOf("key" to key)) { res -> res.code }
    }

    // ---------------- 收藏 ----------------
    suspend fun getFavorites(): Map<String, Favorite> {
        return call("GET", "/api/favorites") { res ->
            val j = parseObject(res)
            val out = mutableMapOf<String, Favorite>()
            j.forEach { (k, v) ->
                (v as? JsonObject)?.let { out[k] = Favorite.fromJson(it) }
            }
            out
        }
    }

    suspend fun saveFavorite(key: String, f: Favorite) {
        call("POST", "/api/favorites",
            body = mapOf("key" to key, "favorite" to f.toJsonMap())) { res -> res.code }
    }

    suspend fun deleteFavorite(key: String) {
        call("DELETE", "/api/favorites", query = mapOf("key" to key)) { res -> res.code }
    }

    // ---------------- 播放记录 ----------------
    suspend fun getPlayRecords(): Map<String, PlayRecord> {
        return call("GET", "/api/playrecords") { res ->
            val j = parseObject(res)
            val out = mutableMapOf<String, PlayRecord>()
            j.forEach { (k, v) ->
                (v as? JsonObject)?.let { out[k] = PlayRecord.fromJson(it) }
            }
            out
        }
    }

    suspend fun savePlayRecord(key: String, r: PlayRecord) {
        call("POST", "/api/playrecords",
            body = mapOf("key" to key, "record" to r.toJsonMap())) { res -> res.code }
    }

    suspend fun deletePlayRecord(key: String) {
        call("DELETE", "/api/playrecords", query = mapOf("key" to key)) { res -> res.code }
    }

    // ---------------- 搜索历史 ----------------
    suspend fun getSearchHistory(): List<String> {
        return call("GET", "/api/searchhistory") { res ->
            parseStringList(res)
        }
    }

    suspend fun addSearchHistory(keyword: String): List<String> {
        return call("POST", "/api/searchhistory",
            body = mapOf("keyword" to keyword)) { res ->
            parseStringList(res)
        }
    }

    suspend fun deleteSearchHistory(keyword: String) {
        call("DELETE", "/api/searchhistory",
            query = mapOf("keyword" to keyword)) { res -> res.code }
    }

    // ---------------- 今日新更 ----------------
    suspend fun getTodayUpdated(): TodayUpdatedRecord? {
        return call("GET", "/api/today-updated") { res ->
            val j = parseObject(res)
            if (j["items"] != null) TodayUpdatedRecord.fromJson(j) else null
        }
    }

    // ---------------- 管理后台 ----------------
    /** 读取管理员配置（仅 owner/admin，401 无权限）。返回 {Role, Config}。 */
    suspend fun getAdminConfig(): JsonObject {
        return call("GET", "/api/admin/config") { res -> parseObject(res) }
    }

    /** 保存站点配置（透传 SiteConfig 字段）。 */
    suspend fun saveAdminSite(site: Map<String, Any?>) {
        call("POST", "/api/admin/site", body = site) { res -> res.code }
    }

    /** 数据源管理。action: add/disable/enable/delete/sort/batch*。 */
    suspend fun adminSource(
        action: String,
        key: String? = null,
        name: String? = null,
        api: String? = null,
        detail: String? = null,
    ) {
        val body = mutableMapOf<String, Any?>("action" to action)
        key?.let { body["key"] = it }
        name?.let { body["name"] = it }
        api?.let { body["api"] = it }
        detail?.let { body["detail"] = it }
        call("POST", "/api/admin/source", body = body) { res -> res.code }
    }

    /** 自定义分类管理。action: add/disable/enable/delete/sort。 */
    suspend fun adminCategory(
        action: String,
        name: String? = null,
        type: String? = null,
        query: String? = null,
    ) {
        val body = mutableMapOf<String, Any?>("action" to action)
        name?.let { body["name"] = it }
        type?.let { body["type"] = it }
        query?.let { body["query"] = it }
        call("POST", "/api/admin/category", body = body) { res -> res.code }
    }

    /** 用户管理。action: setAllowRegister/add/ban/unban/setAdmin/cancelAdmin/changePassword/deleteUser。 */
    suspend fun adminUser(
        action: String,
        targetUsername: String? = null,
        targetPassword: String? = null,
        allowRegister: Boolean? = null,
    ) {
        val body = mutableMapOf<String, Any?>("action" to action)
        targetUsername?.let { body["targetUsername"] = it }
        targetPassword?.let { body["targetPassword"] = it }
        allowRegister?.let { body["allowRegister"] = it }
        call("POST", "/api/admin/user", body = body) { res -> res.code }
    }
}

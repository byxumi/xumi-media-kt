package com.xumitech.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** API 错误:携带 HTTP 状态码与服务端返回体。 */
class ApiException(val code: Int, val body: String) : Exception("HTTP $code: $body")

/**
 * 后端契约客户端:直接对应 moontv-src 的 41 个 API 路由。
 * - 认证:登录/注册后捕获 Set-Cookie `auth=...`,后续请求携带。
 * - 解析:返回 kotlinx.serialization 树形结构,由调用方转为 Model。
 */
object MoonApi {
    const val DEFAULT_SERVER = "https://tv.xumitech.top"
    private const val DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    @Volatile
    var server: String = DEFAULT_SERVER
        private set

    @Volatile
    private var authCookie: String? = null

    var onAuthChanged: (() -> Unit)? = null

    fun configure(newServer: String, cookie: String?) {
        server = newServer.trimEnd('/')
        authCookie = cookie
    }

    fun currentCookie(): String? = authCookie

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private suspend fun call(
        method: String,
        path: String,
        withCookie: Boolean = true,
        query: Map<String, String> = emptyMap(),
        body: JsonElement? = null,
    ): JsonElement = withContext(Dispatchers.IO) {
        val url = buildUrl(path, query)
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", DEFAULT_UA)
            .header("Accept", "application/json")
        if (withCookie) {
            authCookie?.let { builder.header("Cookie", it) }
        }
        if (method == "POST" || method == "PUT" || method == "DELETE") {
            val requestBody = (body?.toString() ?: "{}").toRequestBody(JSON_MEDIA)
            builder.method(method, requestBody)
        }
        val resp = client.newCall(builder.build()).execute()
        val bodyText = resp.body?.string() ?: ""
        if (withCookie) {
            resp.headers["Set-Cookie"]?.let { setCookie ->
                val m = Regex("auth=([^;]+)").find(setCookie)
                if (m != null) {
                    authCookie = "auth=${m.groupValues[1]}"
                    onAuthChanged?.invoke()
                }
            }
        }
        if (!resp.isSuccessful) {
            throw ApiException(resp.code, bodyText.take(300))
        }
        try {
            json.parseToJsonElement(bodyText)
        } catch (_: Exception) {
            JsonPrimitive(bodyText)
        }
    }

    private fun buildUrl(path: String, query: Map<String, String>): String {
        val base = server.trimEnd('/') + path
        if (query.isEmpty()) return base
        val sb = StringBuilder(base)
        var first = true
        query.forEach { (k, v) ->
            if (!first) sb.append('&')
            first = false
            sb.append(k).append('=').append(URLEncoder.encode(v, "UTF-8"))
        }
        return sb.toString()
    }

    // ---------------- 认证 ----------------
    suspend fun login(username: String, password: String): String {
        call(
            "POST", "/api/login", withCookie = false,
            body = buildJsonObject { put("username", username); put("password", password) },
        )
        return currentCookie() ?: ""
    }

    suspend fun register(username: String, password: String): String {
        call(
            "POST", "/api/register", withCookie = false,
            body = buildJsonObject { put("username", username); put("password", password) },
        )
        return currentCookie() ?: ""
    }

    suspend fun logout() {
        try {
            call("POST", "/api/logout")
        } catch (_: Exception) {
        }
        authCookie = null
        onAuthChanged?.invoke()
    }

    // ---------------- 配置 ----------------
    suspend fun getServerConfig(): ServerConfig =
        ServerConfig.fromJson(call("GET", "/api/server-config", withCookie = false).jsonObject)

    suspend fun getSources(): List<Source> {
        val j = call("GET", "/api/config/sources").jsonObject
        val arr = j["items"] as? JsonArray ?: JsonArray(emptyList())
        return arr.mapNotNull { (it as? JsonObject)?.let { o -> Source.fromJson(o) } }
    }

    // ---------------- 详情 / 搜索 ----------------
    suspend fun detail(id: String, source: String): SearchResult =
        SearchResult.fromJson(call("GET", "/api/detail", query = mapOf("id" to id, "source" to source)).jsonObject)

    suspend fun searchVideo(q: String, stream: String = "0"): List<SearchResult> {
        val j = call("GET", "/api/search", query = mapOf("q" to q, "stream" to stream)).jsonObject
        val results = j["results"] as? JsonArray ?: JsonArray(emptyList())
        return results.mapNotNull { (it as? JsonObject)?.let { o -> SearchResult.fromJson(o) } }
    }

    suspend fun searchSuggestions(q: String): List<String> {
        val j = call("GET", "/api/search/suggestions", query = mapOf("q" to q)).jsonObject
        val arr = j["suggestions"] as? JsonArray ?: JsonArray(emptyList())
        return arr.mapNotNull {
            ((it as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() }
        }
    }

    // ---------------- 收藏 / 追更 / 记录 / 搜索历史 ----------------
    suspend fun getFavorites(): Map<String, Favorite> =
        (call("GET", "/api/favorites").jsonObject).entries.associate { (k, v) ->
            k to Favorite.fromJson((v as? JsonObject) ?: JsonObject(emptyMap()))
        }

    suspend fun saveFavorite(key: String, favorite: Favorite) {
        call("POST", "/api/favorites", body = buildJsonObject {
            put("key", key)
            put("favorite", mapToJson(favorite.toJsonMap()))
        })
    }

    suspend fun deleteFavorite(key: String) {
        call("DELETE", "/api/favorites", query = mapOf("key" to key))
    }

    suspend fun getFollowings(): Map<String, Following> =
        (call("GET", "/api/followings").jsonObject).entries.associate { (k, v) ->
            k to Following.fromJson((v as? JsonObject) ?: JsonObject(emptyMap()))
        }

    suspend fun saveFollowing(key: String, following: Following) {
        call("POST", "/api/followings", body = buildJsonObject {
            put("key", key)
            put("following", mapToJson(following.toJsonMap()))
        })
    }

    suspend fun deleteFollowing(key: String) {
        call("DELETE", "/api/followings", query = mapOf("key" to key))
    }

    suspend fun getPlayRecords(): Map<String, PlayRecord> =
        (call("GET", "/api/playrecords").jsonObject).entries.associate { (k, v) ->
            k to PlayRecord.fromJson((v as? JsonObject) ?: JsonObject(emptyMap()))
        }

    suspend fun savePlayRecord(key: String, record: PlayRecord) {
        call("POST", "/api/playrecords", body = buildJsonObject {
            put("key", key)
            put("record", mapToJson(record.toJsonMap()))
        })
    }

    suspend fun deletePlayRecord(key: String) {
        call("DELETE", "/api/playrecords", query = mapOf("key" to key))
    }

    suspend fun getSearchHistory(): List<String> = parseStringArray(call("GET", "/api/searchhistory").jsonObject)

    suspend fun addSearchHistory(keyword: String): List<String> =
        parseStringArray(call("POST", "/api/searchhistory", body = buildJsonObject { put("keyword", keyword) }).jsonObject)

    suspend fun deleteSearchHistory(keyword: String): List<String> =
        parseStringArray(call("DELETE", "/api/searchhistory", query = mapOf("keyword" to keyword)).jsonObject)

    // ---------------- 豆瓣 / 今日新更 ----------------
    suspend fun getDoubanRecommends(kind: String, limit: Int = 20, start: Int = 0): List<DoubanItem> {
        val j = call(
            "GET", "/api/douban/recommends",
            query = mapOf("kind" to kind, "limit" to limit.toString(), "start" to start.toString()),
        ).jsonObject
        val list = j["list"] as? JsonArray ?: JsonArray(emptyList())
        return list.mapNotNull { (it as? JsonObject)?.let { o -> DoubanItem.fromJson(o) } }
    }

    suspend fun getDoubanCategories(
        kind: String, category: String = "", type: String = "",
        limit: Int = 16, start: Int = 0,
    ): List<DoubanItem> {
        val q = buildMap {
            put("kind", kind)
            if (category.isNotBlank()) put("category", category)
            if (type.isNotBlank()) put("type", type)
            put("limit", limit.toString())
            put("start", start.toString())
        }
        val j = call("GET", "/api/douban/categories", query = q).jsonObject
        val list = j["list"] as? JsonArray ?: JsonArray(emptyList())
        return list.mapNotNull { (it as? JsonObject)?.let { o -> DoubanItem.fromJson(o) } }
    }

    suspend fun getTodayUpdated(): TodayUpdatedRecord? {
        val j = call("GET", "/api/today-updated").jsonObject
        return if (j["items"] != null) TodayUpdatedRecord.fromJson(j) else null
    }

    // ---------------- 账号 ----------------
    suspend fun changePassword(oldPassword: String, newPassword: String) {
        call("POST", "/api/change-password", body = buildJsonObject {
            put("oldPassword", oldPassword)
            put("newPassword", newPassword)
        })
    }

    // ---------------- 管理 ----------------
    suspend fun getAdminConfig(): JsonObject =
        call("GET", "/api/admin/config").jsonObject

    suspend fun saveAdminSite(site: Map<String, Any?>) {
        call("POST", "/api/admin/site", body = mapToJson(site))
    }

    suspend fun adminSource(
        action: String,
        key: String? = null,
        name: String? = null,
        api: String? = null,
        detail: String? = null,
    ) {
        call("POST", "/api/admin/source", body = buildJsonObject {
            put("action", action)
            key?.let { put("key", it) }
            name?.let { put("name", it) }
            api?.let { put("api", it) }
            detail?.let { put("detail", it) }
        })
    }

    suspend fun adminCategory(action: String, name: String? = null, type: String? = null, query: String? = null) {
        call("POST", "/api/admin/category", body = buildJsonObject {
            put("action", action)
            name?.let { put("name", it) }
            type?.let { put("type", it) }
            query?.let { put("query", it) }
        })
    }

    suspend fun adminUser(
        action: String,
        targetUsername: String? = null,
        targetPassword: String? = null,
        allowRegister: Boolean? = null,
    ) {
        call("POST", "/api/admin/user", body = buildJsonObject {
            put("action", action)
            targetUsername?.let { put("targetUsername", it) }
            targetPassword?.let { put("targetPassword", it) }
            allowRegister?.let { put("allowRegister", it) }
        })
    }

    // ---------------- 解析辅助 ----------------
    private fun mapToJson(m: Map<String, Any?>): JsonObject = JsonObject(
        m.mapValues { (_, v) ->
            when (v) {
                null -> JsonNull
                is Number -> JsonPrimitive(v)
                is Boolean -> JsonPrimitive(v)
                is String -> JsonPrimitive(v)
                else -> JsonPrimitive(v.toString())
            }
        },
    )

    private fun parseStringArray(j: JsonObject): List<String> {
        val arr = j["items"] as? JsonArray ?: j["keywords"] as? JsonArray ?: JsonArray(emptyList())
        return arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    }
}
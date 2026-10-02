package com.xumitech.tv.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** 搜索结果 / 详情(一个源的一条影片,含该源全部集数)。 */
@Serializable
data class SearchResult(
    val id: String = "",
    val title: String = "",
    val poster: String = "",
    val episodes: List<String> = emptyList(),
    val episodesTitles: List<String> = emptyList(),
    val source: String = "",
    val sourceName: String = "",
    val vodClass: String = "",
    val year: String = "",
    val desc: String = "",
    val typeName: String = "",
    val doubanId: Int = 0,
) {
    val key: String get() = "$source+$id"
    fun episodeTitle(i: Int): String {
        if (i >= 0 && i < episodesTitles.size) {
            val t = episodesTitles[i]
            if (t.isNotBlank()) return t
        }
        return if (episodes.size > 1) "第${i + 1}集" else "正片"
    }

    companion object {
        fun fromJson(j: JsonObject): SearchResult = SearchResult(
            id = j.str("id"),
            title = j.str("title").trim(),
            poster = j.str("poster"),
            episodes = j.strList("episodes"),
            episodesTitles = j.strList("episodes_titles"),
            source = j.str("source"),
            sourceName = j.str("source_name"),
            vodClass = j.str("class"),
            year = j.str("year"),
            desc = j.str("desc"),
            typeName = j.str("type_name"),
            doubanId = j.intOf("douban_id"),
        )
    }
}

/** 豆瓣推荐条目。 */
@Serializable
data class DoubanItem(
    val id: String = "",
    val title: String = "",
    val poster: String = "",
    val source: String = "",
    val sourceName: String = "",
    val year: String = "",
    val rate: String = "",
    val doubanId: Int = 0,
) {
    companion object {
        fun fromJson(j: JsonObject): DoubanItem = DoubanItem(
            id = j.str("id"),
            title = j.str("title").trim(),
            poster = j.str("poster"),
            source = j.str("source"),
            sourceName = j.str("source_name"),
            year = j.str("year"),
            rate = j.str("rate"),
            doubanId = j.intOf("douban_id"),
        )
    }
}

/** 播放源(ApiSite,来自 /api/config/sources)。 */
@Serializable
data class Source(
    val key: String = "",
    val name: String = "",
    val api: String = "",
    val detail: String? = null,
) {
    /** 播放时用作 Referer 的地址(优先 detail,其次 api 的 host)。 */
    val referer: String
        get() {
            val candidate = if (!detail.isNullOrEmpty()) detail else api
            return try {
                val u = java.net.URI(candidate)
                if (!u.host.isNullOrEmpty()) "${u.scheme}://${u.host}/" else ""
            } catch (_: Exception) {
                ""
            }
        }

    companion object {
        fun fromJson(j: JsonObject): Source = Source(
            key = j.str("key"),
            name = j.str("name"),
            api = j.str("api"),
            detail = j["detail"]?.jsonPrimitive?.contentOrNull,
        )
    }
}

/** 收藏。 */
@Serializable
data class Favorite(
    val id: String = "",
    val source: String = "",
    val title: String = "",
    val sourceName: String = "",
    val cover: String = "",
    val year: String = "",
    val saveTime: Long = 0,
    val searchTitle: String = "",
) {
    fun toJsonMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "source" to source,
        "title" to title,
        "source_name" to sourceName,
        "cover" to cover,
        "year" to year,
        "save_time" to saveTime,
        "search_title" to searchTitle,
    )

    companion object {
        fun fromJson(j: JsonObject): Favorite = Favorite(
            id = j.str("id"),
            source = j.str("source"),
            title = j.str("title"),
            sourceName = j.str("source_name"),
            cover = j.str("cover"),
            year = j.str("year"),
            saveTime = j.longOf("save_time"),
            searchTitle = j.str("search_title"),
        )
    }
}

/** 播放记录。 */
@Serializable
data class PlayRecord(
    val id: String = "",
    val source: String = "",
    val title: String = "",
    val sourceName: String = "",
    val cover: String = "",
    val year: String = "",
    val index: Int = 1,
    val totalEpisodes: Int = 0,
    val playTime: Int = 0,
    val totalTime: Int = 0,
    val saveTime: Long = 0,
    val searchTitle: String = "",
) {
    fun toJsonMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "source" to source,
        "title" to title,
        "source_name" to sourceName,
        "cover" to cover,
        "year" to year,
        "index" to index,
        "total_episodes" to totalEpisodes,
        "play_time" to playTime,
        "total_time" to totalTime,
        "save_time" to saveTime,
        "search_title" to searchTitle,
    )

    companion object {
        fun fromJson(j: JsonObject): PlayRecord = PlayRecord(
            id = j.str("id"),
            source = j.str("source"),
            title = j.str("title"),
            sourceName = j.str("source_name"),
            cover = j.str("cover"),
            year = j.str("year"),
            index = j.intOf("index", 1),
            totalEpisodes = j.intOf("total_episodes"),
            playTime = j.intOf("play_time"),
            totalTime = j.intOf("total_time"),
            saveTime = j.longOf("save_time"),
            searchTitle = j.str("search_title"),
        )
    }
}

/** 追更。 */
@Serializable
data class Following(
    val id: String = "",
    val source: String = "",
    val sourceName: String = "",
    val totalEpisodes: Int = 0,
    val watchedEpisodes: Int = 0,
    val title: String = "",
    val year: String = "",
    val cover: String = "",
    val saveTime: Long = 0,
    val searchTitle: String = "",
) {
    fun toJsonMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "source" to source,
        "source_name" to sourceName,
        "total_episodes" to totalEpisodes,
        "watched_episodes" to watchedEpisodes,
        "title" to title,
        "year" to year,
        "cover" to cover,
        "save_time" to saveTime,
        "search_title" to searchTitle,
    )

    companion object {
        fun fromJson(j: JsonObject): Following = Following(
            id = j.str("id"),
            source = j.str("source"),
            sourceName = j.str("source_name"),
            totalEpisodes = j.intOf("total_episodes"),
            watchedEpisodes = j.intOf("watched_episodes"),
            title = j.str("title"),
            year = j.str("year"),
            cover = j.str("cover"),
            saveTime = j.longOf("save_time"),
            searchTitle = j.str("search_title"),
        )
    }
}

/** 服务端配置。 */
@Serializable
data class ServerConfig(
    val siteName: String = "",
    val version: String = "",
) {
    companion object {
        fun fromJson(j: JsonObject): ServerConfig = ServerConfig(
            siteName = j.str("SiteName", j.str("siteName", j.str("site_name"))),
            version = j.str("version"),
        )
    }
}

/** 今日新更条目。 */
@Serializable
data class TodayUpdatedItem(
    val source: String = "",
    val id: String = "",
    val title: String = "",
    val poster: String = "",
    val episodes: Int = 0,
    val watchedEpisodes: Int = 0,
    val unwatchedEpisodes: Int = 0,
    val oldEpisodes: Int = 0,
    val newEpisodes: Int = 0,
    val sourceName: String = "",
    val year: String = "",
)

/** 今日新更记录。 */
@Serializable
data class TodayUpdatedRecord(
    val date: String = "",
    val items: List<TodayUpdatedItem> = emptyList(),
) {
    companion object {
        fun fromJson(j: JsonObject): TodayUpdatedRecord = TodayUpdatedRecord(
            date = j.str("date"),
            items = (j["items"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.jsonObject }
                ?.map { o ->
                    TodayUpdatedItem(
                        source = o.str("source"),
                        id = o.str("id"),
                        title = o.str("title"),
                        poster = o.str("poster"),
                        episodes = o.intOf("episodes"),
                        watchedEpisodes = o.intOf("watchedEpisodes"),
                        unwatchedEpisodes = o.intOf("unwatchedEpisodes"),
                        oldEpisodes = o.intOf("oldEpisodes"),
                        newEpisodes = o.intOf("newEpisodes"),
                        sourceName = o.str("source_name"),
                        year = o.str("year"),
                    )
                } ?: emptyList(),
        )
    }
}

/** 客户端聚合分组:同一影片在多个源下的结果归为一组。 */
data class VideoGroup(
    val title: String,
    val poster: String,
    val year: String,
    val typeName: String,
    val desc: String,
    val sources: List<SearchResult>,
) {
    companion object {
        fun normTitle(t: String): String {
            var s = t.trim()
            s = s.replace(Regex("\\[[^\\]]*\\]"), "")
            s = s.replace(Regex("（[^）]*）"), "")
            s = s.replace(Regex("\\([^)]*\\)"), "")
            s = s.replace(Regex("\\s+"), "")
            return s.lowercase()
        }

        /** 把搜索结果按影片聚合(优先豆瓣 id,其次规范化标题),同一源只保留集数最多的一条。 */
        fun group(results: List<SearchResult>): List<VideoGroup> {
            val map = linkedMapOf<String, MutableList<SearchResult>>()
            for (r in results) {
                val key = if (r.doubanId > 0) "d${r.doubanId}" else "n${normTitle(r.title)}"
                map.getOrPut(key) { mutableListOf() }.add(r)
            }
            val groups = mutableListOf<VideoGroup>()
            for ((_, list) in map) {
                val first = list.first()
                val best = linkedMapOf<String, SearchResult>()
                for (r in list) {
                    val cur = best[r.source]
                    if (cur == null || r.episodes.size > cur.episodes.size) {
                        best[r.source] = r
                    }
                }
                val uniq = best.values.toList()
                groups += VideoGroup(
                    title = first.title,
                    poster = first.poster,
                    year = first.year,
                    typeName = first.typeName,
                    desc = pickIntro(uniq, fallback = first.desc),
                    sources = uniq,
                )
            }
            return groups
        }

        fun pickIntro(sources: List<SearchResult>, fallback: String = ""): String {
            for (s in sources) {
                val d = cleanDesc(s.desc)
                if (d.isEmpty() || isPlaceholder(d)) continue
                return d
            }
            return cleanDesc(fallback)
        }

        fun cleanDesc(input: String): String {
            if (input.isEmpty()) return ""
            return input
                .replace(Regex("<[^>]+>"), "")
                .replace("&nbsp;", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun isPlaceholder(d: String): Boolean {
            if (d.length <= 4) return true
            return Regex(
                "暂无简介|剧情简介暂缺|敬请期待|该片(?:暂无|没有|尚未).{0,8}简介|即将上映|敬请观看|更新中",
                RegexOption.IGNORE_CASE,
            ).containsMatchIn(d)
        }
    }
}

// ---------------- JSON 解析辅助 ----------------
internal fun JsonObject.str(key: String, fallback: String = ""): String =
    (this[key]?.jsonPrimitive?.contentOrNull ?: fallback)

internal fun JsonObject.strList(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

internal fun JsonObject.intOf(key: String, default: Int = 0): Int =
    (this[key]?.jsonPrimitive?.contentOrNull ?: default.toString()).toIntOrNull() ?: default

internal fun JsonObject.longOf(key: String, default: Long = 0): Long =
    (this[key]?.jsonPrimitive?.contentOrNull ?: default.toString()).toLongOrNull() ?: default

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}
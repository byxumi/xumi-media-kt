package com.xumitech.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** 测速结果（含分辨率，学 MoonTV getVideoResolutionFromM3u8 的分辨率分级）。 */
data class SpeedResult(
    val ok: Boolean,
    val speedKBps: Double,
    val pingMs: Int,
    val quality: String, // 4K / 2K / 1080p / 720p / 480p / SD / 未知
    val resolution: String, // 如 1920x1080
) {
    val speedText: String
        get() {
            if (!ok) return "失败"
            if (speedKBps >= 1024) return "%.1f MB/s".format(speedKBps / 1024)
            if (speedKBps > 0) return "%.0f KB/s".format(speedKBps)
            return "未知"
        }

    val pingText: String get() = if (pingMs > 0) "$pingMs ms" else "—"

    companion object {
        val FAIL = SpeedResult(false, 0.0, -1, "未知", "")
    }
}

/** 分辨率 -> 清晰度等级（与 MoonTV 一致）。 */
fun qualityOfResolution(res: String): String {
    val m = Regex("(\\d{3,4})x(\\d{3,4})").find(res) ?: return "未知"
    val w = m.groupValues[1].toIntOrNull() ?: 0
    return when {
        w >= 3840 -> "4K"
        w >= 2560 -> "2K"
        w >= 1920 -> "1080p"
        w >= 1280 -> "720p"
        w >= 854 -> "480p"
        else -> "SD"
    }
}

/** 主播放列表解析结果：最佳分辨率 + 子播放列表地址。 */
private data class MasterInfo(val resolution: String?, val children: List<String>)

private fun parseMasterPlaylist(playlist: String): MasterInfo {
    val infs = Regex("#EXT-X-STREAM-INF:([^\\n]*)").findAll(playlist).toList()
    if (infs.isEmpty()) return MasterInfo(null, emptyList())
    var bestBandwidth: Int? = null
    var bestRes: String? = null
    val childUrls = mutableListOf<String>()
    for (m in infs) {
        val info = m.groupValues[1]
        val bw = Regex("BANDWIDTH=(\\d+)").find(info)?.groupValues[1]?.toIntOrNull()
        val res = Regex("RESOLUTION=(\\d+x\\d+)").find(info)?.groupValues[1]
        if (res != null && (bestBandwidth == null || (bw != null && bw > bestBandwidth))) {
            bestBandwidth = bw ?: bestBandwidth
            bestRes = res
        }
        val lineAfter = playlist.substring(m.range.last + 1)
        val next = Regex("^\\s*([^\\s#][^\\n]*)").find(lineAfter)
        val child = next?.groupValues?.get(1)?.trim()
        if (!child.isNullOrEmpty()) childUrls.add(child)
    }
    return MasterInfo(bestRes, childUrls)
}

/** 播放源测速：拉 m3u8 测延迟 + 解析分辨率，再拉首个 TS 分片测下载速度。 */
object SpeedTester {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /** suspend 版本：强制 IO 线程，避免主线程 NetworkOnMainThreadException。 */
    suspend fun testSpeed(url: String, referer: String = ""): SpeedResult =
        withContext(Dispatchers.IO) {
            testSpeedSync(url, referer)
        }

    private fun testSpeedSync(url: String, referer: String = ""): SpeedResult {
        val baseUrl = url.toHttpUrlOrNull() ?: return SpeedResult.FAIL
        return try {
            // 1. 拉 m3u8 测延迟
            val t0 = System.nanoTime()
            val body = fetch(baseUrl, referer) ?: return SpeedResult.FAIL
            val pingMs = ((System.nanoTime() - t0) / 1_000_000).toInt()

            // 2. 解析分辨率（主列表优先，递归子列表深 2）
            var res: String? = null
            var currentBody = body
            var currentBase = baseUrl
            var depth = 0
            while (res == null && depth < 2) {
                val parsed = parseMasterPlaylist(currentBody)
                if (parsed.resolution != null) {
                    res = parsed.resolution
                    break
                }
                if (parsed.children.isEmpty()) break
                val child = parsed.children.first()
                val childUrl = currentBase.resolve(child) ?: break
                val childBody = fetch(childUrl, referer) ?: break
                currentBody = childBody
                currentBase = childUrl
                depth++
            }
            val quality = if (res != null) qualityOfResolution(res) else "未知"

            // 3. 找首个分片测下载速度（在最终使用的播放列表里找）
            var segUrl: HttpUrl? = null
            for (line in currentBody.split("\n")) {
                val t = line.trim()
                if (t.isEmpty() || t.startsWith("#")) continue
                segUrl = currentBase.resolve(t)
                break
            }
            if (segUrl == null) {
                return SpeedResult(true, 0.0, pingMs, quality, res ?: "")
            }
            val t1 = System.nanoTime()
            val bytes = fetchBytes(segUrl, referer)?.size ?: 0
            val ms = ((System.nanoTime() - t1) / 1_000_000).toInt()
            val speed = if (ms > 0) bytes * 8.0 / 1024.0 / (ms / 1000.0) else 0.0
            SpeedResult(true, speed, pingMs, quality, res ?: "")
        } catch (_: Exception) {
            SpeedResult.FAIL
        }
    }

    private fun fetch(url: HttpUrl, referer: String): String? {
        val rb = Request.Builder().url(url)
            .header("User-Agent", MoonTvApi.UA)
            .header("Accept", "*/*")
        if (referer.isNotEmpty()) rb.header("Referer", referer)
        client.newCall(rb.build()).execute().use { res ->
            if (!res.isSuccessful) return null
            return res.body?.string()
        }
    }

    private fun fetchBytes(url: HttpUrl, referer: String): ByteArray? {
        val rb = Request.Builder().url(url)
            .header("User-Agent", MoonTvApi.UA)
            .header("Accept", "*/*")
        if (referer.isNotEmpty()) rb.header("Referer", referer)
        client.newCall(rb.build()).execute().use { res ->
            if (!res.isSuccessful) return null
            return res.body?.bytes()
        }
    }
}

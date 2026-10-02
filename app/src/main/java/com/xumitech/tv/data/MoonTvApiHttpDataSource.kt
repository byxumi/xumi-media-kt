package com.xumitech.tv.data

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource

/**
 * 播放 m3u8 时使用的 HttpDataSource 工厂:
 * 携带 User-Agent + Referer(源站防盗链必需,与 Flutter 版一致)。
 */
object MoonTvApiHttpDataSource {

    fun factory(context: Context, referer: String): DataSource.Factory {
        val props = mutableMapOf<String, String>()
        if (referer.isNotEmpty()) props["Referer"] = referer
        val base = DefaultHttpDataSource.Factory()
            .setUserAgent(MoonApiUA)
            .setDefaultRequestProperties(props)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(20_000)

        return DefaultDataSource.Factory(context, base)
    }

    private const val MoonApiUA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}

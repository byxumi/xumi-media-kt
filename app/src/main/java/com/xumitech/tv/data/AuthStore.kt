package com.xumitech.tv.data

import android.content.Context

/** 会话与偏好持久化:服务器地址 / auth cookie / 主题模式。 */
class AuthStore(context: Context) {
    private val prefs = context.getSharedPreferences("xumi_session", Context.MODE_PRIVATE)

    var server: String
        get() = prefs.getString("server", MoonApi.DEFAULT_SERVER) ?: MoonApi.DEFAULT_SERVER
        set(value) = prefs.edit().putString("server", value).apply()

    var cookie: String?
        get() = prefs.getString("cookie", null)
        set(value) {
            prefs.edit().putString("cookie", value).apply()
        }

    var themeMode: String
        get() = prefs.getString("theme_mode", "system") ?: "system"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    fun clear() {
        prefs.edit().remove("cookie").apply()
    }
}
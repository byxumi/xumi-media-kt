package com.xumitech.tv.data

import android.content.Context
import android.content.SharedPreferences

/** 登录态持久化：服务器地址 + auth cookie + 用户名。 */
class AuthStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("xumi_tv", Context.MODE_PRIVATE)

    var server: String
        get() = prefs.getString(KEY_SERVER, MoonTvApi.DEFAULT_SERVER) ?: MoonTvApi.DEFAULT_SERVER
        set(v) {
            prefs.edit().putString(KEY_SERVER, v).apply()
        }

    var cookie: String
        get() = prefs.getString(KEY_COOKIE, "") ?: ""
        set(v) {
            prefs.edit().putString(KEY_COOKIE, v).apply()
        }

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(v) {
            prefs.edit().putString(KEY_USERNAME, v).apply()
        }

    var role: String
        get() = prefs.getString(KEY_ROLE, "") ?: ""
        set(v) {
            prefs.edit().putString(KEY_ROLE, v).apply()
        }

    /** 启动时恢复登录态。 */
    fun restore() {
        MoonTvApi.configure(server, cookie.takeIf { it.isNotEmpty() })
        val r = MoonTvApi.parseRole()
        if (r.isNotEmpty()) role = r
    }

    fun saveAuth(cookie: String, username: String) {
        this.cookie = cookie
        this.username = username
        val r = MoonTvApi.parseRole()
        role = r
    }

    fun clear() {
        cookie = ""
        role = ""
        username = ""
    }

    companion object {
        private const val KEY_SERVER = "server"
        private const val KEY_COOKIE = "auth_cookie"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
    }
}

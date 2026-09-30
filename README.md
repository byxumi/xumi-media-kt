# 须弥Media (XumiMedia) — Kotlin 客户端

> MoonTV 后端的 **Kotlin (Compose)** 客户端，采用 **AndroidLiquidGlass 真·液态玻璃导航栏**。

## 特性

- 🧊 **AndroidLiquidGlass 液态导航栏**：基于 `io.github.kyant0:backdrop`（Kyant0/AndroidLiquidGlass 官方发布物）实现真·液态玻璃 — 背后内容实时折射 (lens) + 模糊 (blur) + 增艳 (vibrancy)，选中胶囊弹性流动 + 色散 (chromatic aberration)
- 🔐 **完整用户系统**：登录 / 注册 / 退出，收藏、播放记录、搜索历史与站点云端同步
- 🔍 **多源聚合搜索**：后端聚合全部采集源，按影片去重分组，标注源数量
- ⚡ **优选测速**：进入播放自动测速（延迟 + 下载速度 + 分辨率），按「清晰度 40% + 速度 40% + 延迟 20%」评分自动优选
- ▶️ **流畅播放**：ExoPlayer (Media3) 内核，HLS/m3u8、自动连播、进度记忆、防盗链 Referer
- ❤️ **收藏 + 续播 + 追更**：云端收藏、观看历史、断点续播、追更提醒
- 🌗 **深色影视风 UI**：月光紫渐变 + 液态毛玻璃卡片 + 微光加载
- 🏆 **分类榜单页**：电影/剧集/综艺/动漫 + 类型 chips + 分页网格

## 技术栈

| 层 | 技术 |
|---|---|
| 客户端 | Kotlin 2.4 + Jetpack Compose (M3) + Material Icons Ext |
| 液态玻璃 | `io.github.kyant0:backdrop:2.0.1` (AndroidLiquidGlass 的 Compose 库) |
| 网络 | OkHttp 4.12 + kotlinx.serialization |
| 播放 | Media3 ExoPlayer (HLS) |
| 图片 | Coil 2.7 |
| 后端 | MoonTV Next.js (TypeScript)，`https://tv.xumitech.top` |

## 构建

GitHub Actions 自动构建：push 到 `main` 即触发 `.github/workflows/build-apk.yml`，产物发布到 `latest-apk` Release。

本地构建：
```bash
export JAVA_HOME=<JDK17>
export ANDROID_HOME=<SDK>
./gradlew :app:assembleRelease
```

## 目录结构

```
app/src/main/java/com/xumitech/tv/
├── MainActivity.kt          # 入口 + 液态导航 + 页面路由
├── AppState.kt              # 全局状态（auth/首页/收藏/记录）
├── data/
│   ├── MoonTvApi.kt         # 后端 API 客户端（cookie 鉴权，修复序列化 bug）
│   ├── SpeedTester.kt       # 测速 + 分辨率解析
│   ├── SourceScorer.kt      # 优选评分
│   └── AuthStore.kt         # 登录态持久化
├── model/Models.kt          # 数据模型 + VideoGroup 聚合
└── ui/
    ├── components/
    │   ├── LiquidNavBar.kt  # AndroidLiquidGlass 真·液态导航栏
    │   ├── Common.kt        # 玻璃卡片/微光/骨架/空状态
    │   └── Cards.kt         # 海报卡/继续观看卡
    └── screens/             # 登录/首页/搜索/详情/播放/收藏/历史/我的/榜单
```

## 免责声明

本项目仅提供影视信息聚合与搜索，不存储任何视频资源，所有内容来自第三方接口。请遵守当地法律法规，仅供学习交流。
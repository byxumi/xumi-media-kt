package com.xumitech.tv.data

import kotlin.math.min

/**
 * 播放源评分（学 MoonTV play-utils.calculateSourceScore）：
 * 清晰度 40% + 下载速度 40% + 网络延迟 20%。
 */
object SourceScorer {

    fun scoreOf(r: SpeedResult, all: List<SpeedResult>): Double {
        if (!r.ok) return -1.0

        // 分辨率评分（40% 权重）
        val qualityScore = when (r.quality) {
            "4K" -> 100.0
            "2K" -> 85.0
            "1080p" -> 75.0
            "720p" -> 60.0
            "480p" -> 40.0
            "SD" -> 20.0
            else -> 0.0
        }

        // 下载速度评分（40% 权重）：基于最大速度线性映射
        val maxSpeed = all.asSequence()
            .filter { it.ok && it.speedKBps > 0 }
            .map { it.speedKBps }
            .maxOrNull() ?: 1024.0
        val speedScore = if (r.speedKBps > 0) {
            min(100.0, r.speedKBps / maxSpeed * 100)
        } else {
            30.0 // 无有效速度给默认分（与 MoonTV「未知」一致）
        }

        // 网络延迟评分（20% 权重）：最低延迟 100 分，最高 0 分
        val pings = all.asSequence()
            .filter { it.ok && it.pingMs > 0 }
            .map { it.pingMs }
            .toList()
        val pingScore: Double = when {
            r.pingMs <= 0 || pings.isEmpty() -> 0.0
            else -> {
                val minPing = pings.minOrNull()!!
                val maxPing = pings.maxOrNull()!!
                if (maxPing == minPing) 100.0
                else (maxPing - r.pingMs).toDouble() / (maxPing - minPing) * 100
            }
        }

        return qualityScore * 0.4 + speedScore * 0.4 + pingScore * 0.2
    }
}
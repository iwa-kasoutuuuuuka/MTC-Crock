package com.mtc.crock.util

/**
 * アプリ全体で使用する定数クラス（マジックナンバー排他）
 */
object Constants {
    // 焼き付き防止設定
    const val BURN_IN_SHIFT_INTERVAL_MS = 300_000L // 5分 (300,000ms)
    const val BURN_IN_COLOR_CHANGE_INTERVAL_MS = 7_200_000L // 2時間 (7,200,000ms)
    const val MAX_PIXEL_SHIFT_OFFSET_PX = 8.0f // ランダム移動最大ピクセル数

    // 明るさ設定デフォルト値
    const val DEFAULT_DAY_BRIGHTNESS = 1.0f // 100%
    const val DEFAULT_NIGHT_BRIGHTNESS = 0.2f // 20%
    const val DEFAULT_DAY_START_HOUR = 7
    const val DEFAULT_NIGHT_START_HOUR = 19
    const val BRIGHTNESS_OVERLAY_TIMEOUT_MS = 2500L

    // スマート充電設定
    const val CHARGE_STOP_THRESHOLD = 80 // 80%で充電停止
    const val CHARGE_RESUME_THRESHOLD = 75 // 75%で充電再開

    // 天気更新設定
    const val DEFAULT_WEATHER_CITY = "Tokyo"
    const val DEFAULT_WEATHER_LATITUDE = 35.6762
    const val DEFAULT_WEATHER_LONGITUDE = 139.6503
    const val DEFAULT_WEATHER_REFRESH_MINUTES = 30

    // NTP同期
    const val NTP_SERVER_HOST = "pool.ntp.org"
    const val NTP_TIMEOUT_MS = 3000
    const val NTP_SYNC_INTERVAL_MS = 3_600_000L // 1時間ごと

    // アニメーション・描画
    const val FRAME_RATE_60_FPS_MS = 16L
}

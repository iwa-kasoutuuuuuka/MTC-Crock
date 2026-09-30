package com.mtc.crock.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.mtc.crock.data.TemperatureUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 摂氏(°C)のDouble値を指定された単位(°C/°F)に変換・フォーマットする拡張関数
 */
fun Double.toFormattedTemperature(unit: TemperatureUnit): String {
    val value = if (unit == TemperatureUnit.FAHRENHEIT) {
        this * 9.0 / 5.0 + 32.0
    } else {
        this
    }
    return "${value.formatOneDecimal()}${unit.symbol}"
}

/**
 * 摂氏(°C)のDouble値を指定された単位(°C/°F)に変換し、整数値＋記号にフォーマットする拡張関数 (例: 72°F)
 */
fun Double.toFormattedIntTemperature(unit: TemperatureUnit): String {
    val value = if (unit == TemperatureUnit.FAHRENHEIT) {
        this * 9.0 / 5.0 + 32.0
    } else {
        this
    }
    return "${Math.round(value)}${unit.symbol}"
}

/**
 * 摂氏(°C)のFloat値を指定された単位(°C/°F)に変換・フォーマットする拡張関数
 */
fun Float.toFormattedTemperature(unit: TemperatureUnit): String {
    return this.toDouble().toFormattedTemperature(unit)
}

/**
 * Double値を指定した小数位の文字列にフォーマットする拡張関数
 */
fun Double.formatOneDecimal(): String {
    return String.format(Locale.getDefault(), "%.1f", this)
}

/**
 * Int値をパーセント表記にフォーマットする拡張関数
 */
fun Int.toPercentString(): String {
    return "$this%"
}

/**
 * Dateオブジェクトを西暦日付（YYYY/MM/DD）に変換する拡張関数
 */
fun Date.toFormattedDateString(): String {
    val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return formatter.format(this)
}

/**
 * Dateオブジェクトから時刻文字列（HH:mm）に変換する拡張関数
 */
fun Date.toTimeString(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(this)
}

/**
 * Dateオブジェクトから曜日（日本語表記または英語表記）を取得する拡張関数
 */
fun Date.toDayOfWeekString(): String {
    val formatter = SimpleDateFormat("EEE", Locale.JAPANESE)
    return formatter.format(this)
}

/**
 * DateオブジェクトからAM/PMを取得する拡張関数
 */
fun Date.toAmPmString(): String {
    val formatter = SimpleDateFormat("a", Locale.US)
    return formatter.format(this)
}

/**
 * Wi-Fi接続中かどうかをチェックする拡張関数
 */
fun Context.isWifiConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

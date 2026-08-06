package com.mtc.crock.weather

import android.content.Context
import com.google.gson.Gson
import com.mtc.crock.network.WeatherApiService
import com.mtc.crock.util.isWifiConnected
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 天気情報の取得およびキャッシュ管理リポジトリ
 */
class WeatherRepository(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("mtc_weather_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val apiService: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    /**
     * 最新の天気情報を取得する。Wi-Fi非接続時または通信失敗時は直前のキャッシュを返す。
     */
    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherInfo {
        if (context.isWifiConnected()) {
            try {
                val response = apiService.getWeather(latitude, longitude)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val currentWeather = body.currentWeather
                    val hourly = body.hourly

                    val code = currentWeather?.weatherCode ?: 0
                    val (conditionText, iconSymbol) = parseWmoWeatherCode(code)
                    val humidity = hourly?.humidityList?.getOrNull(0) ?: 50
                    val precipProb = hourly?.precipitationProbabilityList?.getOrNull(0) ?: 0
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                    val newInfo = WeatherInfo(
                        conditionText = conditionText,
                        weatherIconSymbol = iconSymbol,
                        temperatureCelsius = currentWeather?.temperature ?: 20.0,
                        humidityPercent = humidity,
                        precipitationProbabilityPercent = precipProb,
                        windSpeedMps = currentWeather?.windSpeed ?: 0.0,
                        lastUpdatedText = "更新: $timeStr (Wi-Fi)",
                        isOfflineData = false
                    )

                    // キャッシュへの保存
                    saveToCache(newInfo)
                    return newInfo
                }
            } catch (e: Exception) {
                // ネットワークエラー時はキャッシュへフォールバック
            }
        }

        // Wi-Fi未接続またはエラー時：キャッシュデータ返却
        val cached = getFromCache()
        return cached.copy(
            isOfflineData = true,
            lastUpdatedText = if (cached.lastUpdatedText.contains("オフライン")) cached.lastUpdatedText else "${cached.lastUpdatedText} [オフライン]"
        )
    }

    private fun saveToCache(info: WeatherInfo) {
        val json = gson.toJson(info)
        sharedPreferences.edit().putString("cached_weather_info", json).apply()
    }

    private fun getFromCache(): WeatherInfo {
        val json = sharedPreferences.getString("cached_weather_info", null)
        if (json != null) {
            try {
                return gson.fromJson(json, WeatherInfo::class.java)
            } catch (e: Exception) {
                // デコード失敗時デフォルト
            }
        }
        return WeatherInfo(lastUpdatedText = "データなし [オフライン]")
    }

    /**
     * WMO Weather interpretation codes (WW) の解析変換
     */
    private fun parseWmoWeatherCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("快晴", "☀️")
            1, 2, 3 -> Pair("晴れ/薄曇り", "⛅")
            45, 48 -> Pair("霧", "🌫️")
            51, 53, 55 -> Pair("霧雨", "🌧️")
            61, 63, 65 -> Pair("雨", "☔")
            71, 73, 75 -> Pair("雪", "❄️")
            80, 81, 82 -> Pair("にわか雨", "🌦️")
            95, 96, 99 -> Pair("雷雨", "⚡")
            else -> Pair("晴れ", "☀️")
        }
    }
}

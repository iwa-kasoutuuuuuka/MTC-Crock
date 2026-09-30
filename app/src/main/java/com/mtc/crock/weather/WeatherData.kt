package com.mtc.crock.weather

import com.google.gson.annotations.SerializedName

/**
 * Open-Meteo REST API から取得する天気レスポンス構造
 */
data class OpenMeteoResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeatherDto?,
    @SerializedName("hourly") val hourly: HourlyWeatherDto?
)

data class CurrentWeatherDto(
    @SerializedName("temperature") val temperature: Double,
    @SerializedName("windspeed") val windSpeed: Double,
    @SerializedName("weathercode") val weatherCode: Int
)

data class HourlyWeatherDto(
    @SerializedName("relativehumidity_2m") val humidityList: List<Int>?,
    @SerializedName("precipitation_probability") val precipitationProbabilityList: List<Int>?
)

/**
 * アプリUI用表示データ構造
 */
data class WeatherInfo(
    val conditionText: String = "晴れ",
    val conditionTextEn: String = "Sunny",
    val weatherIconSymbol: String = "☀️",
    val temperatureCelsius: Double = 22.5,
    val humidityPercent: Int = 50,
    val precipitationProbabilityPercent: Int = 10,
    val windSpeedMps: Double = 2.4,
    val lastUpdatedText: String = "最終更新: 未設定",
    val isOfflineData: Boolean = false
)

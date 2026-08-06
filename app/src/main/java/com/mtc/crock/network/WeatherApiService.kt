package com.mtc.crock.network

import com.mtc.crock.weather.OpenMeteoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo REST API インターフェース (完全無料・APIキー不要)
 */
interface WeatherApiService {

    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("hourly") hourly: String = "relativehumidity_2m,precipitation_probability",
        @Query("timezone") timezone: String = "auto"
    ): Response<OpenMeteoResponse>
}

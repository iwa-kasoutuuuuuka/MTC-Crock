package com.mtc.crock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mtc.crock.theme.AppTheme
import com.mtc.crock.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mtc_crock_settings")

/**
 * 温度単位 Enum
 */
enum class TemperatureUnit(val symbol: String, val displayName: String) {
    CELSIUS("°C", "摂氏 (°C)"),
    FAHRENHEIT("°F", "華氏 (°F)")
}

/**
 * アプリ設定データクラス
 */
data class UserSettings(
    val showDayOfWeek: Boolean = true,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val showWeather: Boolean = true,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val theme: AppTheme = AppTheme.SPORT,
    val dayBrightness: Float = Constants.DEFAULT_DAY_BRIGHTNESS,
    val nightBrightness: Float = Constants.DEFAULT_NIGHT_BRIGHTNESS,
    val dayStartHour: Int = Constants.DEFAULT_DAY_START_HOUR,
    val nightStartHour: Int = Constants.DEFAULT_NIGHT_START_HOUR,
    val weatherCity: String = Constants.DEFAULT_WEATHER_CITY,
    val weatherLatitude: Double = Constants.DEFAULT_WEATHER_LATITUDE,
    val weatherLongitude: Double = Constants.DEFAULT_WEATHER_LONGITUDE,
    val weatherRefreshMinutes: Int = Constants.DEFAULT_WEATHER_REFRESH_MINUTES,
    val enableBurnInProtection: Boolean = true,
    val burnInShiftMinutes: Int = 5,
    val enableColorChange: Boolean = true
)

/**
 * Preferences DataStore による設定の管理リポジトリ
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val SHOW_DAY_OF_WEEK = booleanPreferencesKey("show_day_of_week")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val SHOW_BATTERY = booleanPreferencesKey("show_battery")
        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val THEME_NAME = stringPreferencesKey("theme_name")
        val DAY_BRIGHTNESS = floatPreferencesKey("day_brightness")
        val NIGHT_BRIGHTNESS = floatPreferencesKey("night_brightness")
        val DAY_START_HOUR = intPreferencesKey("day_start_hour")
        val NIGHT_START_HOUR = intPreferencesKey("night_start_hour")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val WEATHER_LAT = floatPreferencesKey("weather_lat")
        val WEATHER_LON = floatPreferencesKey("weather_lon")
        val WEATHER_REFRESH_MIN = intPreferencesKey("weather_refresh_min")
        val BURN_IN_PROTECTION = booleanPreferencesKey("burn_in_protection")
        val BURN_IN_SHIFT_MIN = intPreferencesKey("burn_in_shift_min")
        val COLOR_CHANGE = booleanPreferencesKey("color_change")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            showDayOfWeek = prefs[Keys.SHOW_DAY_OF_WEEK] ?: true,
            showDate = prefs[Keys.SHOW_DATE] ?: true,
            showBattery = prefs[Keys.SHOW_BATTERY] ?: true,
            showWeather = prefs[Keys.SHOW_WEATHER] ?: true,
            temperatureUnit = parseTempUnit(prefs[Keys.TEMP_UNIT]),
            theme = parseTheme(prefs[Keys.THEME_NAME]),
            dayBrightness = prefs[Keys.DAY_BRIGHTNESS] ?: Constants.DEFAULT_DAY_BRIGHTNESS,
            nightBrightness = prefs[Keys.NIGHT_BRIGHTNESS] ?: Constants.DEFAULT_NIGHT_BRIGHTNESS,
            dayStartHour = prefs[Keys.DAY_START_HOUR] ?: Constants.DEFAULT_DAY_START_HOUR,
            nightStartHour = prefs[Keys.NIGHT_START_HOUR] ?: Constants.DEFAULT_NIGHT_START_HOUR,
            weatherCity = prefs[Keys.WEATHER_CITY] ?: Constants.DEFAULT_WEATHER_CITY,
            weatherLatitude = (prefs[Keys.WEATHER_LAT] ?: Constants.DEFAULT_WEATHER_LATITUDE.toFloat()).toDouble(),
            weatherLongitude = (prefs[Keys.WEATHER_LON] ?: Constants.DEFAULT_WEATHER_LONGITUDE.toFloat()).toDouble(),
            weatherRefreshMinutes = prefs[Keys.WEATHER_REFRESH_MIN] ?: Constants.DEFAULT_WEATHER_REFRESH_MINUTES,
            enableBurnInProtection = prefs[Keys.BURN_IN_PROTECTION] ?: true,
            burnInShiftMinutes = prefs[Keys.BURN_IN_SHIFT_MIN] ?: 5,
            enableColorChange = prefs[Keys.COLOR_CHANGE] ?: true
        )
    }

    suspend fun updateDisplaySettings(
        showDayOfWeek: Boolean? = null,
        showDate: Boolean? = null,
        showBattery: Boolean? = null,
        showWeather: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            showDayOfWeek?.let { prefs[Keys.SHOW_DAY_OF_WEEK] = it }
            showDate?.let { prefs[Keys.SHOW_DATE] = it }
            showBattery?.let { prefs[Keys.SHOW_BATTERY] = it }
            showWeather?.let { prefs[Keys.SHOW_WEATHER] = it }
        }
    }

    suspend fun updateTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEMP_UNIT] = unit.name
        }
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_NAME] = theme.name
        }
    }

    suspend fun updateBrightnessSettings(
        dayBrightness: Float? = null,
        nightBrightness: Float? = null,
        dayStartHour: Int? = null,
        nightStartHour: Int? = null
    ) {
        context.dataStore.edit { prefs ->
            dayBrightness?.let { prefs[Keys.DAY_BRIGHTNESS] = it }
            nightBrightness?.let { prefs[Keys.NIGHT_BRIGHTNESS] = it }
            dayStartHour?.let { prefs[Keys.DAY_START_HOUR] = it }
            nightStartHour?.let { prefs[Keys.NIGHT_START_HOUR] = it }
        }
    }

    suspend fun updateWeatherSettings(
        city: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        refreshMinutes: Int? = null
    ) {
        context.dataStore.edit { prefs ->
            city?.let { prefs[Keys.WEATHER_CITY] = it }
            lat?.let { prefs[Keys.WEATHER_LAT] = it.toFloat() }
            lon?.let { prefs[Keys.WEATHER_LON] = it.toFloat() }
            refreshMinutes?.let { prefs[Keys.WEATHER_REFRESH_MIN] = it }
        }
    }

    suspend fun updateBurnInSettings(
        enableBurnIn: Boolean? = null,
        shiftMinutes: Int? = null,
        enableColorChange: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            enableBurnIn?.let { prefs[Keys.BURN_IN_PROTECTION] = it }
            shiftMinutes?.let { prefs[Keys.BURN_IN_SHIFT_MIN] = it }
            enableColorChange?.let { prefs[Keys.COLOR_CHANGE] = it }
        }
    }

    private fun parseTempUnit(name: String?): TemperatureUnit {
        return try {
            if (name != null) TemperatureUnit.valueOf(name) else TemperatureUnit.CELSIUS
        } catch (e: Exception) {
            TemperatureUnit.CELSIUS
        }
    }

    private fun parseTheme(name: String?): AppTheme {
        return try {
            if (name != null) AppTheme.valueOf(name) else AppTheme.SPORT
        } catch (e: Exception) {
            AppTheme.SPORT
        }
    }
}

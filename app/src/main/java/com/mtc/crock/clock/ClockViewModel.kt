package com.mtc.crock.clock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.crock.data.SettingsRepository
import com.mtc.crock.data.UserSettings
import com.mtc.crock.sensor.BatteryAndSensorState
import com.mtc.crock.sensor.BatteryManagerHelper
import com.mtc.crock.util.Constants
import com.mtc.crock.util.NtpSyncHelper
import com.mtc.crock.util.isWifiConnected
import com.mtc.crock.weather.WeatherInfo
import com.mtc.crock.weather.WeatherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.random.Random

/**
 * 焼き付き防止用オフセットデータ
 */
data class BurnInOffset(
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
    val colorToneShift: Float = 0.0f
)

/**
 * 時計画面全体のUI状態
 */
data class ClockUiState(
    val currentTime: Date = Date(),
    val hourAngleDegree: Float = 0.0f,
    val minuteAngleDegree: Float = 0.0f,
    val isNightTime: Boolean = false,
    val currentAutoBrightness: Float = 1.0f,
    val burnInOffset: BurnInOffset = BurnInOffset(),
    val ntpOffsetMs: Long = 0L,
    val weatherInfo: WeatherInfo = WeatherInfo(),
    val batteryState: BatteryAndSensorState = BatteryAndSensorState()
)

/**
 * 時計描画およびデータ統括 ViewModel
 */
class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val weatherRepository = WeatherRepository(application)
    private val batteryManagerHelper = BatteryManagerHelper(application)

    val userSettings: StateFlow<UserSettings> = settingsRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, UserSettings())

    private val _uiState = MutableStateFlow(ClockUiState())
    val uiState: StateFlow<ClockUiState> = _uiState.asStateFlow()

    init {
        batteryManagerHelper.startListening()
        observeBatteryState()
        startClockLoop()
        startBurnInProtectionLoop()
        startNtpAndWeatherLoop()
    }

    private fun observeBatteryState() {
        viewModelScope.launch {
            batteryManagerHelper.state.collect { bState ->
                _uiState.value = _uiState.value.copy(batteryState = bState)
            }
        }
    }

    /**
     * 時計針の滑らかな移動ループ (60fps目標)
     */
    private fun startClockLoop() {
        viewModelScope.launch {
            while (isActive) {
                val nowMs = System.currentTimeMillis() + _uiState.value.ntpOffsetMs
                val calendar = Calendar.getInstance().apply { timeInMillis = nowMs }

                val hour = calendar.get(Calendar.HOUR)
                val minute = calendar.get(Calendar.MINUTE)
                val second = calendar.get(Calendar.SECOND)
                val millisecond = calendar.get(Calendar.MILLISECOND)

                // 針角度の算定 (時針・分針のなだらかな連動スイープ)
                val hourDegree = (hour % 12) * 30.0f + minute * 0.5f + second * (0.5f / 60f)
                val minuteDegree = minute * 6.0f + second * 0.1f

                // 昼夜自動明るさ制御判定
                val currentHour24 = calendar.get(Calendar.HOUR_OF_DAY)
                val settings = userSettings.value
                val isNight = if (settings.dayStartHour < settings.nightStartHour) {
                    currentHour24 < settings.dayStartHour || currentHour24 >= settings.nightStartHour
                } else {
                    currentHour24 in settings.nightStartHour until settings.dayStartHour
                }

                val autoBrightness = if (isNight) settings.nightBrightness else settings.dayBrightness

                _uiState.value = _uiState.value.copy(
                    currentTime = Date(nowMs),
                    hourAngleDegree = hourDegree,
                    minuteAngleDegree = minuteDegree,
                    isNightTime = isNight,
                    currentAutoBrightness = autoBrightness
                )

                delay(Constants.FRAME_RATE_60_FPS_MS)
            }
        }
    }

    /**
     * 有機EL焼き付き防止ループ (5分ごとのピクセルシフト & 色変化)
     */
    private fun startBurnInProtectionLoop() {
        viewModelScope.launch {
            var colorShiftCounter = 0
            while (isActive) {
                val settings = userSettings.value
                val shiftMinMs = settings.burnInShiftMinutes * 60_000L

                delay(if (shiftMinMs > 0) shiftMinMs else Constants.BURN_IN_SHIFT_INTERVAL_MS)

                if (settings.enableBurnInProtection) {
                    val randomX = (Random.nextFloat() * 2 - 1) * Constants.MAX_PIXEL_SHIFT_OFFSET_PX
                    val randomY = (Random.nextFloat() * 2 - 1) * Constants.MAX_PIXEL_SHIFT_OFFSET_PX

                    colorShiftCounter++
                    val toneShift = if (settings.enableColorChange && colorShiftCounter % 24 == 0) {
                        Random.nextFloat() * 0.1f // 微小なトーンシフト
                    } else {
                        _uiState.value.burnInOffset.colorToneShift
                    }

                    _uiState.value = _uiState.value.copy(
                        burnInOffset = BurnInOffset(randomX, randomY, toneShift)
                    )
                }
            }
        }
    }

    /**
     * NTP時刻同期および天気情報定期更新ループ
     */
    private fun startNtpAndWeatherLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (getApplication<Application>().isWifiConnected()) {
                    // NTP同期
                    val offset = NtpSyncHelper.fetchNtpOffset()
                    _uiState.value = _uiState.value.copy(ntpOffsetMs = offset)

                    // 天気更新
                    val settings = userSettings.value
                    val wInfo = weatherRepository.fetchWeather(settings.weatherLatitude, settings.weatherLongitude)
                    _uiState.value = _uiState.value.copy(weatherInfo = wInfo)
                }
                val intervalMs = userSettings.value.weatherRefreshMinutes * 60_000L
                delay(if (intervalMs > 0) intervalMs else Constants.NTP_SYNC_INTERVAL_MS)
            }
        }
    }

    fun refreshWeatherManually() {
        viewModelScope.launch {
            val settings = userSettings.value
            val wInfo = weatherRepository.fetchWeather(settings.weatherLatitude, settings.weatherLongitude)
            _uiState.value = _uiState.value.copy(weatherInfo = wInfo)
        }
    }

    override fun onCleared() {
        super.onCleared()
        batteryManagerHelper.stopListening()
    }
}

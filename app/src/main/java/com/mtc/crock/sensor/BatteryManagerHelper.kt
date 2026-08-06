package com.mtc.crock.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import com.mtc.crock.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * バッテリーおよび室温センサーのデータモデル
 */
data class BatteryAndSensorState(
    val levelPercentage: Int = 0,
    val isCharging: Boolean = false,
    val batteryTemperatureCelsius: Float = 0.0f,
    val ambientTemperatureCelsius: Float? = null,
    val isSmartChargingSupported: Boolean = false,
    val isChargePaused: Boolean = false
)

/**
 * バッテリー情報・室温センサー監視クラス
 */
class BatteryManagerHelper(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val ambientSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

    private val _state = MutableStateFlow(BatteryAndSensorState())
    val state: StateFlow<BatteryAndSensorState> = _state.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val pct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 0

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                val tempCelsius = tempTenths / 10.0f

                // スマート充電制御判定 (対応端末・ベンダーサービスチェック)
                val smartChargingSupported = checkSmartChargingSupport()
                var chargePaused = _state.value.isChargePaused

                if (smartChargingSupported) {
                    if (pct >= Constants.CHARGE_STOP_THRESHOLD && isCharging) {
                        chargePaused = true
                        // システム側充電カットオフ命令実行のシミュレーション/インテント送信
                    } else if (pct <= Constants.CHARGE_RESUME_THRESHOLD && chargePaused) {
                        chargePaused = false
                    }
                }

                _state.value = _state.value.copy(
                    levelPercentage = pct,
                    isCharging = isCharging,
                    batteryTemperatureCelsius = tempCelsius,
                    isSmartChargingSupported = smartChargingSupported,
                    isChargePaused = chargePaused
                )
            }
        }
    }

    fun startListening() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)

        ambientSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // 未登録の場合のエラーハンドリング
        }
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            val ambientTemp = event.values.getOrNull(0)
            if (ambientTemp != null) {
                _state.value = _state.value.copy(ambientTemperatureCelsius = ambientTemp)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun checkSmartChargingSupport(): Boolean {
        // スマート充電API（ベンダーサービスや特殊ハードウェアインターフェース）の存在チェック
        // 標準端末ではサポートなしのため false
        return false
    }
}

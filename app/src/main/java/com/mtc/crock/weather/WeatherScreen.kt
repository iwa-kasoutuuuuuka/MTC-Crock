package com.mtc.crock.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtc.crock.clock.ClockUiState
import com.mtc.crock.data.UserSettings
import com.mtc.crock.theme.BlackBackground
import com.mtc.crock.theme.CharcoalDial
import com.mtc.crock.theme.DialTextWhite
import com.mtc.crock.theme.SubTextGray
import com.mtc.crock.util.formatOneDecimal
import com.mtc.crock.util.toFormattedTemperature

/**
 * 詳細天気情報表示画面
 */
@Composable
fun WeatherScreen(
    uiState: ClockUiState,
    userSettings: UserSettings,
    onRefreshWeather: () -> Unit,
    onBackToClock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weather = uiState.weatherInfo

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BlackBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⛅ 気象情報 (${userSettings.weatherCity})",
                color = DialTextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onBackToClock,
                colors = ButtonDefaults.buttonColors(containerColor = userSettings.theme.accentColor)
            ) {
                Text("時計に戻る ↩", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // メイン天気カード
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalDial),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = weather.weatherIconSymbol, fontSize = 72.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = weather.conditionText,
                    color = DialTextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = weather.temperatureCelsius.toFormattedTemperature(userSettings.temperatureUnit),
                    color = userSettings.theme.accentColor,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    WeatherMetricItem(label = "💧 湿度", value = "${weather.humidityPercent}%")
                    WeatherMetricItem(label = "☔ 降水確率", value = "${weather.precipitationProbabilityPercent}%")
                    WeatherMetricItem(label = "🌬️ 風速", value = "${weather.windSpeedMps.formatOneDecimal()} m/s")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = weather.lastUpdatedText, color = SubTextGray, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRefreshWeather,
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalDial)
                ) {
                    Text("🔄 今すぐ手動更新 (Wi-Fi必要)", color = DialTextWhite)
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = SubTextGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = DialTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

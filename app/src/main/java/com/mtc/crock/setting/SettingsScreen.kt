package com.mtc.crock.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtc.crock.data.SettingsRepository
import com.mtc.crock.data.TemperatureUnit
import com.mtc.crock.data.UserSettings
import com.mtc.crock.theme.AppTheme
import com.mtc.crock.theme.BlackBackground
import com.mtc.crock.theme.CharcoalDial
import com.mtc.crock.theme.DialTextWhite
import com.mtc.crock.theme.SubTextGray
import kotlinx.coroutines.launch

/**
 * 設定画面 Compose UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userSettings: UserSettings,
    settingsRepository: SettingsRepository,
    onBackToClock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BlackBackground)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // ヘッダーバー
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚙️ MTC-Crock 設定",
                color = DialTextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onBackToClock,
                colors = ButtonDefaults.buttonColors(containerColor = userSettings.theme.accentColor)
            ) {
                Text("時計に戻る ↩", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. 表示 ON/OFF 設定カード
        SettingSectionCard(title = "📺 表示 & 単位設定") {
            SwitchSettingRow("曜日の表示", userSettings.showDayOfWeek) {
                scope.launch { settingsRepository.updateDisplaySettings(showDayOfWeek = it) }
            }
            SwitchSettingRow("日付の表示", userSettings.showDate) {
                scope.launch { settingsRepository.updateDisplaySettings(showDate = it) }
            }
            SwitchSettingRow("バッテリー情報の表示", userSettings.showBattery) {
                scope.launch { settingsRepository.updateDisplaySettings(showBattery = it) }
            }
            SwitchSettingRow("天気情報の表示", userSettings.showWeather) {
                scope.launch { settingsRepository.updateDisplaySettings(showWeather = it) }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("🌡️ 温度単位設定", color = DialTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TemperatureUnit.values().forEach { unit ->
                    Button(
                        onClick = { scope.launch { settingsRepository.updateTemperatureUnit(unit) } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (userSettings.temperatureUnit == unit) userSettings.theme.accentColor else CharcoalDial
                        )
                    ) {
                        Text(unit.displayName, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 時計・デザインテーマ設定カード
        SettingSectionCard(title = "🎨 時計 & テーマ設定") {
            Text("テーマ選択", color = DialTextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTheme.values().forEach { themeOption ->
                    Button(
                        onClick = { scope.launch { settingsRepository.updateTheme(themeOption) } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (userSettings.theme == themeOption) themeOption.accentColor else CharcoalDial
                        )
                    ) {
                        Text(themeOption.displayName, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 昼間明るさ
            Text("昼間明るさ (${(userSettings.dayBrightness * 100).toInt()}%)", color = DialTextWhite)
            Slider(
                value = userSettings.dayBrightness,
                onValueChange = { scope.launch { settingsRepository.updateBrightnessSettings(dayBrightness = it) } },
                valueRange = 0.05f..1.0f
            )

            // 夜間明るさ
            Text("夜間明るさ (${(userSettings.nightBrightness * 100).toInt()}%)", color = DialTextWhite)
            Slider(
                value = userSettings.nightBrightness,
                onValueChange = { scope.launch { settingsRepository.updateBrightnessSettings(nightBrightness = it) } },
                valueRange = 0.05f..1.0f
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("昼開始時間: ${userSettings.dayStartHour}:00", color = SubTextGray)
                Text("夜開始時間: ${userSettings.nightStartHour}:00", color = SubTextGray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 天気設定カード
        SettingSectionCard(title = "⛅ 天気設定") {
            var cityInput by remember { mutableStateOf(userSettings.weatherCity) }
            OutlinedTextField(
                value = cityInput,
                onValueChange = { cityInput = it },
                label = { Text("地域/都市名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { scope.launch { settingsRepository.updateWeatherSettings(city = cityInput) } },
                colors = ButtonDefaults.buttonColors(containerColor = userSettings.theme.accentColor)
            ) {
                Text("都市名を保存", color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("自動更新間隔: ${userSettings.weatherRefreshMinutes}分", color = DialTextWhite)
            Slider(
                value = userSettings.weatherRefreshMinutes.toFloat(),
                onValueChange = { scope.launch { settingsRepository.updateWeatherSettings(refreshMinutes = it.toInt()) } },
                valueRange = 10f..120f,
                steps = 10
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 焼き付き防止設定カード
        SettingSectionCard(title = "🛡️ 有機EL焼き付き防止設定") {
            SwitchSettingRow("ピクセルシフト (位置移動)", userSettings.enableBurnInProtection) {
                scope.launch { settingsRepository.updateBurnInSettings(enableBurnIn = it) }
            }
            SwitchSettingRow("自動トーンカラー微調整", userSettings.enableColorChange) {
                scope.launch { settingsRepository.updateBurnInSettings(enableColorChange = it) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("移動実行間隔: ${userSettings.burnInShiftMinutes}分ごと", color = DialTextWhite)
            Slider(
                value = userSettings.burnInShiftMinutes.toFloat(),
                onValueChange = { scope.launch { settingsRepository.updateBurnInSettings(shiftMinutes = it.toInt()) } },
                valueRange = 1f..30f,
                steps = 29
            )
        }
    }
}

@Composable
private fun SettingSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalDial),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = DialTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SwitchSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = DialTextWhite, fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = androidx.compose.ui.graphics.Color(0xFFDC2626),
                checkedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                uncheckedThumbColor = androidx.compose.ui.graphics.Color(0xFF9CA3AF),
                uncheckedTrackColor = androidx.compose.ui.graphics.Color(0xFF374151),
                uncheckedBorderColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
    }
}

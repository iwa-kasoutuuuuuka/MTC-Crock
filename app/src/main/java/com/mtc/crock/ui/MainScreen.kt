package com.mtc.crock.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtc.crock.clock.ClockViewModel
import com.mtc.crock.clock.SpeedometerClock
import com.mtc.crock.data.SettingsRepository
import com.mtc.crock.gesture.GestureContainer
import com.mtc.crock.setting.SettingsScreen
import com.mtc.crock.theme.BlackBackground
import com.mtc.crock.theme.CharcoalDial
import com.mtc.crock.theme.DialTextWhite
import com.mtc.crock.weather.WeatherScreen

enum class ScreenState {
    CLOCK, SETTINGS, WEATHER
}

/**
 * MTC-Crock アプリのメインナビゲーション & 画面統括コンポーネント
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    viewModel: ClockViewModel,
    settingsRepository: SettingsRepository,
    onBrightnessChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    var currentScreen by remember { mutableStateOf(ScreenState.CLOCK) }
    var showMenuDialog by remember { mutableStateOf(false) }

    val activeBrightness = if (uiState.isNightTime) userSettings.nightBrightness else userSettings.dayBrightness

    GestureContainer(
        onSwipeRight = { currentScreen = ScreenState.SETTINGS },
        onSwipeLeft = { currentScreen = ScreenState.WEATHER },
        onDoubleTap = { currentScreen = ScreenState.CLOCK },
        onLongPress = { showMenuDialog = true },
        onBrightnessChanged = onBrightnessChanged,
        currentBrightness = activeBrightness
    ) {
        Box(modifier = modifier.fillMaxSize().background(BlackBackground)) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() with fadeOut() }
            ) { screen ->
                when (screen) {
                    ScreenState.CLOCK -> {
                        SpeedometerClock(
                            uiState = uiState,
                            userSettings = userSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ScreenState.SETTINGS -> {
                        SettingsScreen(
                            userSettings = userSettings,
                            settingsRepository = settingsRepository,
                            onBackToClock = { currentScreen = ScreenState.CLOCK }
                        )
                    }
                    ScreenState.WEATHER -> {
                        WeatherScreen(
                            uiState = uiState,
                            userSettings = userSettings,
                            onRefreshWeather = { viewModel.refreshWeatherManually() },
                            onBackToClock = { currentScreen = ScreenState.CLOCK }
                        )
                    }
                }
            }

            // 長押し時に表示されるクイックメニューダイアログ
            if (showMenuDialog) {
                AlertDialog(
                    onDismissRequest = { showMenuDialog = false },
                    containerColor = CharcoalDial,
                    title = {
                        Text(
                            text = "🏎️ MTC-Crock メニュー",
                            color = DialTextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    showMenuDialog = false
                                    currentScreen = ScreenState.SETTINGS
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = userSettings.theme.accentColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("⚙️ 設定画面を開く", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    showMenuDialog = false
                                    currentScreen = ScreenState.WEATHER
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalDial),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("⛅ 天気画面を開く", color = DialTextWhite)
                            }
                            Button(
                                onClick = {
                                    showMenuDialog = false
                                    currentScreen = ScreenState.CLOCK
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalDial),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("⏱️ 時計画面に戻る", color = DialTextWhite)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMenuDialog = false }) {
                            Text("閉じる", color = DialTextWhite)
                        }
                    }
                )
            }
        }
    }
}

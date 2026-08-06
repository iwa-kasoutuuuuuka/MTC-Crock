package com.mtc.crock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * MTC-Crockのアニメーション・メーターテーマEnum
 */
enum class AppTheme(val displayName: String, val accentColor: Color, val warningZoneColor: Color) {
    SPORT("SPORT (黒×赤)", SportAccent, SportRedZone),
    RACING("RACING (黒×青)", RacingAccent, RacingRedZone),
    CLASSIC("CLASSIC (黒×緑)", ClassicAccent, ClassicRedZone)
}

private val DarkColorScheme = darkColorScheme(
    primary = SportAccent,
    background = BlackBackground,
    surface = CharcoalDial,
    onBackground = DialTextWhite,
    onSurface = DialTextWhite
)

@Composable
fun MTCCrockTheme(
    appTheme: AppTheme = AppTheme.SPORT,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = appTheme.accentColor,
        background = BlackBackground,
        surface = CharcoalDial,
        onBackground = DialTextWhite,
        onSurface = DialTextWhite
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

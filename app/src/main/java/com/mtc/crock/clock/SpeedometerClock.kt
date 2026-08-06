package com.mtc.crock.clock

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.mtc.crock.data.UserSettings
import com.mtc.crock.theme.BezelBorderColor
import com.mtc.crock.theme.BlackBackground
import com.mtc.crock.theme.CharcoalDial
import com.mtc.crock.theme.DialTextWhite
import com.mtc.crock.theme.GridGreen
import com.mtc.crock.theme.LcdBackground
import com.mtc.crock.theme.LcdTextGreen
import com.mtc.crock.theme.MetallicRingGradientEnd
import com.mtc.crock.theme.MetallicRingGradientStart
import com.mtc.crock.theme.ScrewColor
import com.mtc.crock.theme.SubTextGray
import com.mtc.crock.theme.VintageYellowCap
import com.mtc.crock.theme.VintageYellowHand
import com.mtc.crock.util.formatOneDecimal
import com.mtc.crock.util.toAmPmString
import com.mtc.crock.util.toDayOfWeekString
import com.mtc.crock.util.toFormattedDateString
import com.mtc.crock.util.toFormattedTemperature
import com.mtc.crock.util.toPercentString
import kotlin.math.cos
import kotlin.math.sin

/**
 * 改訂版 (V5 Below Center): 液晶表示部を針中心（キャップ）より完全に下側の領域のみに精密配置。
 * 80年代の日本精機製レトロバイクメーターデザインを継承したスピードメーター風アナログ時計 Canvas コンポーネント。
 */
@Composable
fun SpeedometerClock(
    uiState: ClockUiState,
    userSettings: UserSettings,
    modifier: Modifier = Modifier
) {
    val textPaintWhite = remember {
        Paint().apply {
            color = DialTextWhite.toArgb()
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val textPaintRed = remember {
        Paint().apply {
            color = userSettings.theme.warningZoneColor.toArgb()
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val lcdTextPaint = remember {
        Paint().apply {
            color = LcdTextGreen.toArgb()
            textSize = 32f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f + uiState.burnInOffset.offsetX, height / 2f + uiState.burnInOffset.offsetY)

            val meterSize = minOf(width, height) * 0.88f
            val meterHalf = meterSize / 2f

            // 1. 角丸スクエア・アウターベゼル (外枠)
            val outerCornerRadius = meterSize * 0.12f
            drawRoundRect(
                brush = Brush.linearGradient(listOf(MetallicRingGradientStart, MetallicRingGradientEnd)),
                topLeft = Offset(center.x - meterHalf, center.y - meterHalf),
                size = Size(meterSize, meterSize),
                cornerRadius = CornerRadius(outerCornerRadius, outerCornerRadius)
            )

            // 2. インナー文字盤 (Charcoal Dial Face)
            val innerSize = meterSize * 0.92f
            val innerHalf = innerSize / 2f
            drawRoundRect(
                color = CharcoalDial,
                topLeft = Offset(center.x - innerHalf, center.y - innerHalf),
                size = Size(innerSize, innerSize),
                cornerRadius = CornerRadius(outerCornerRadius * 0.8f, outerCornerRadius * 0.8f)
            )

            // 3. エメラルドグリーンの格子模様 (Grid Lines)
            val gridStep = innerSize / 8f
            val left = center.x - innerHalf
            val right = center.x + innerHalf
            val top = center.y - innerHalf
            val bottom = center.y + innerHalf

            var x = left + gridStep
            while (x < right) {
                drawLine(
                    color = GridGreen.copy(alpha = 0.35f),
                    start = Offset(x, top),
                    end = Offset(x, bottom),
                    strokeWidth = 2.5f
                )
                x += gridStep
            }
            var y = top + gridStep
            while (y < bottom) {
                drawLine(
                    color = GridGreen.copy(alpha = 0.35f),
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 2.5f
                )
                y += gridStep
            }

            // 4. スピードメーター風 警告帯 (9時〜12時のレッドラインゾーン・アーチ)
            val radius = innerSize * 0.38f
            val redZoneStartAngle = 180f
            val redZoneSweepAngle = 90f

            drawArc(
                color = userSettings.theme.warningZoneColor.copy(alpha = 0.9f),
                startAngle = redZoneStartAngle,
                sweepAngle = redZoneSweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = innerSize * 0.08f, cap = StrokeCap.Butt)
            )

            // 5. 外周の 1〜12 のみの文字盤 (内側の数字は完全に排除)
            for (i in 1..12) {
                val angleDeg = (i * 30f) - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val isRedZone = i in 9..12
                val numText = i.toString()

                val tickOuterR = radius + innerSize * 0.04f
                val tickInnerR = radius - innerSize * 0.04f
                val tickStart = Offset(
                    (center.x + tickInnerR * cos(angleRad)).toFloat(),
                    (center.y + tickInnerR * sin(angleRad)).toFloat()
                )
                val tickEnd = Offset(
                    (center.x + tickOuterR * cos(angleRad)).toFloat(),
                    (center.y + tickOuterR * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = DialTextWhite,
                    start = tickStart,
                    end = tickEnd,
                    strokeWidth = 12f,
                    cap = StrokeCap.Round
                )

                // 中間ドット
                val midAngleRad = Math.toRadians((angleDeg + 15f).toDouble())
                val dotPos = Offset(
                    (center.x + radius * cos(midAngleRad)).toFloat(),
                    (center.y + radius * sin(midAngleRad)).toFloat()
                )
                drawCircle(
                    color = DialTextWhite,
                    radius = 4.5f,
                    center = dotPos
                )

                // 外周数字配置 (FontMetricsと半径の最適化で完全に揃える)
                val numberR = radius - innerSize * 0.02f
                val numberX = (center.x + numberR * cos(angleRad)).toFloat()
                
                val paint = if (isRedZone) textPaintRed else textPaintWhite
                val fontMetrics = paint.fontMetrics
                val yOffset = -(fontMetrics.ascent + fontMetrics.descent) / 2f
                val numberY = (center.y + numberR * sin(angleRad)).toFloat() + yOffset

                drawContext.canvas.nativeCanvas.drawText(numText, numberX, numberY, paint)
            }

            // 6. トリップメーター風 デジタルLCD表示部 (針の中心より完全な下部領域に配置)
            val lcdWidth = innerSize * 0.58f
            val lcdHeight = innerSize * 0.15f
            val lcdTopLeft = Offset(center.x - lcdWidth / 2f, center.y + innerSize * 0.15f)

            drawRoundRect(
                color = LcdBackground,
                topLeft = lcdTopLeft,
                size = Size(lcdWidth, lcdHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = BezelBorderColor,
                topLeft = lcdTopLeft,
                size = Size(lcdWidth, lcdHeight),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(2.5f)
            )

            val dateStr = if (userSettings.showDate) uiState.currentTime.toFormattedDateString() else ""
            val dayOfWeekStr = if (userSettings.showDayOfWeek) "(${uiState.currentTime.toDayOfWeekString()})" else ""
            val amPmStr = uiState.currentTime.toAmPmString()

            val row1Text = "$amPmStr  $dateStr $dayOfWeekStr".trim()
            drawContext.canvas.nativeCanvas.drawText(
                row1Text,
                center.x,
                lcdTopLeft.y + lcdHeight * 0.44f,
                lcdTextPaint
            )

            val weatherStr = if (userSettings.showWeather) {
                "${uiState.weatherInfo.weatherIconSymbol} ${uiState.weatherInfo.temperatureCelsius.toFormattedTemperature(userSettings.temperatureUnit)}"
            } else ""
            val batteryStr = if (userSettings.showBattery) {
                "⚡${uiState.batteryState.levelPercentage.toPercentString()} ${uiState.batteryState.batteryTemperatureCelsius.toFormattedTemperature(userSettings.temperatureUnit)}"
            } else ""

            val row2Text = "$weatherStr   $batteryStr".trim()
            if (row2Text.isNotEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    row2Text,
                    center.x,
                    lcdTopLeft.y + lcdHeight * 0.84f,
                    Paint(lcdTextPaint).apply { textSize = 25f }
                )
            }

            // 7. 下部パーツ: 黒ネジ 2個 (十字スリット付き)
            val screwR = innerSize * 0.035f
            val screwY = center.y + innerSize * 0.33f
            val screwXLeft = center.x - innerSize * 0.22f
            val screwXRight = center.x + innerSize * 0.22f

            listOf(screwXLeft, screwXRight).forEach { sx ->
                drawCircle(color = ScrewColor, radius = screwR, center = Offset(sx, screwY))
                drawCircle(color = BezelBorderColor, radius = screwR, center = Offset(sx, screwY), style = Stroke(2f))
                drawLine(color = SubTextGray, start = Offset(sx - screwR * 0.6f, screwY), end = Offset(sx + screwR * 0.6f, screwY), strokeWidth = 3f)
                drawLine(color = SubTextGray, start = Offset(sx, screwY - screwR * 0.6f), end = Offset(sx, screwY + screwR * 0.6f), strokeWidth = 3f)
            }

            // 8. 時計の針描画 (時針・分針) - より太く力強いデザイン
            val hourLength = innerSize * 0.26f
            val minuteLength = innerSize * 0.40f

            // 時針 (Hour Hand)
            rotate(degrees = uiState.hourAngleDegree - 90f, pivot = center) {
                val path = Path().apply {
                    moveTo(center.x - 12f, center.y)
                    lineTo(center.x + hourLength, center.y - 14f)
                    lineTo(center.x + hourLength + 12f, center.y)
                    lineTo(center.x + hourLength, center.y + 14f)
                    lineTo(center.x - 12f, center.y)
                    close()
                }
                drawPath(path = path, color = VintageYellowHand)
                drawPath(path = path, color = BlackBackground, style = Stroke(3.5f))
            }

            // 分針 (Minute Hand)
            rotate(degrees = uiState.minuteAngleDegree - 90f, pivot = center) {
                val path = Path().apply {
                    moveTo(center.x - 16f, center.y)
                    lineTo(center.x + minuteLength, center.y - 10f)
                    lineTo(center.x + minuteLength + 14f, center.y)
                    lineTo(center.x + minuteLength, center.y + 10f)
                    lineTo(center.x - 16f, center.y)
                    close()
                }
                drawPath(path = path, color = VintageYellowHand)
                drawPath(path = path, color = BlackBackground, style = Stroke(3.5f))
            }

            // 9. イエローセンターハブキャップ
            drawCircle(
                color = VintageYellowCap,
                radius = innerSize * 0.05f,
                center = center
            )
            drawCircle(
                color = BlackBackground,
                radius = innerSize * 0.05f,
                center = center,
                style = Stroke(3f)
            )
            drawCircle(
                color = VintageYellowHand,
                radius = innerSize * 0.02f,
                center = center
            )
        }
    }
}

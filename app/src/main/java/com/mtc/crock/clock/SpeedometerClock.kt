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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.mtc.crock.data.UserSettings
import com.mtc.crock.util.toAmPmString
import com.mtc.crock.util.toDayOfWeekString
import com.mtc.crock.util.toFormattedDateString
import com.mtc.crock.util.toFormattedTemperature
import com.mtc.crock.util.toPercentString
import com.mtc.crock.util.toTimeString
import java.text.SimpleDateFormat
import java.util.Locale

// HONDA モトコンポ (NC50) スピードメーター 完全再現トーン＆カラー
private val BezelTopHighlight = Color(0xFF3C3E42)
private val BezelBodyStart = Color(0xFF26282C)
private val BezelBodyEnd = Color(0xFF141517)
private val BezelWallDark = Color(0xFF040506)
private val BezelWallLight = Color(0xFF1E2023)
private val InnerBezelFrame = Color(0xFF0B0C0D)

private val DialBgColor = Color(0xFF121315)
// ホンダ純正 モトコンポ エメラルドグリーン格子
private val GridLineGreen = Color(0xFF339966).copy(alpha = 0.88f)

private val DialTextWhite = Color(0xFFF3F4F6)
private val DialTextRed = Color(0xFFDC2626) // モトコンポ 30km/h 速度警告レッド

// HONDA 純正マスタードイエロー 3D メーター針
private val HandYellowHighlight = Color(0xFFFDE047)
private val HandYellowMain = Color(0xFFEAB308)
private val HandYellowShadow = Color(0xFFA16207)
private val HandYellowDarkShadow = Color(0xFF543403)
private val HandDropShadow = Color(0xFF000000).copy(alpha = 0.55f)

// 3D ビスカラー
private val ScrewHoleBg = Color(0xFF060708)
private val ScrewHeadStart = Color(0xFF36393D)
private val ScrewHeadEnd = Color(0xFF101113)
private val ScrewSlotShadow = Color(0xFF030304)
private val ScrewSlotHighlight = Color(0xFF4D5055)

// TN液晶カラー (オドメーター風 レトロオリーブセピア)
private val LcdGlassStart = Color(0xFF909A88)
private val LcdGlassEnd = Color(0xFFA2AC9A)
private val LcdFrameShadow = Color(0xFF060708)
private val LcdTextDark = Color(0xFF161C14)
private val LcdTextGhost = Color(0x16161C14)

/**
 * リアル7セグメント液晶文字描画エンジン (角張りデジタルバー)
 */
private fun DrawScope.draw7SegmentDigit(
    digit: Char,
    topLeft: Offset,
    digitWidth: Float,
    digitHeight: Float,
    color: Color,
    thickness: Float = digitWidth * 0.26f,
    skew: Float = digitWidth * 0.08f
) {
    val segMap = mapOf(
        '0' to setOf('A', 'B', 'C', 'D', 'E', 'F'),
        '1' to setOf('B', 'C'),
        '2' to setOf('A', 'B', 'D', 'E', 'G'),
        '3' to setOf('A', 'B', 'C', 'D', 'G'),
        '4' to setOf('B', 'C', 'F', 'G'),
        '5' to setOf('A', 'C', 'D', 'F', 'G'),
        '6' to setOf('A', 'C', 'D', 'E', 'F', 'G'),
        '7' to setOf('A', 'B', 'C'),
        '8' to setOf('A', 'B', 'C', 'D', 'E', 'F', 'G'),
        '9' to setOf('A', 'B', 'C', 'D', 'F', 'G')
    )

    val activeSegs = segMap[digit] ?: emptySet()
    val halfH = digitHeight / 2f
    val t = thickness
    val gap = t * 0.16f

    fun drawSeg(seg: Char) {
        if (seg !in activeSegs) return
        val path = Path()
        when (seg) {
            'A' -> {
                path.moveTo(topLeft.x + gap + skew, topLeft.y)
                path.lineTo(topLeft.x + digitWidth - gap + skew, topLeft.y)
                path.lineTo(topLeft.x + digitWidth - gap - t + skew, topLeft.y + t)
                path.lineTo(topLeft.x + gap + t + skew, topLeft.y + t)
                path.close()
            }
            'B' -> {
                path.moveTo(topLeft.x + digitWidth + skew, topLeft.y + gap)
                path.lineTo(topLeft.x + digitWidth + skew * 0.5f, topLeft.y + halfH - gap / 2f)
                path.lineTo(topLeft.x + digitWidth - t + skew * 0.5f, topLeft.y + halfH - gap / 2f - t * 0.5f)
                path.lineTo(topLeft.x + digitWidth - t + skew, topLeft.y + gap + t)
                path.close()
            }
            'C' -> {
                path.moveTo(topLeft.x + digitWidth + skew * 0.5f, topLeft.y + halfH + gap / 2f)
                path.lineTo(topLeft.x + digitWidth, topLeft.y + digitHeight - gap)
                path.lineTo(topLeft.x + digitWidth - t, topLeft.y + digitHeight - gap - t)
                path.lineTo(topLeft.x + digitWidth - t + skew * 0.5f, topLeft.y + halfH + gap / 2f + t * 0.5f)
                path.close()
            }
            'D' -> {
                path.moveTo(topLeft.x + gap + t, topLeft.y + digitHeight - t)
                path.lineTo(topLeft.x + digitWidth - gap - t, topLeft.y + digitHeight - t)
                path.lineTo(topLeft.x + digitWidth - gap, topLeft.y + digitHeight)
                path.lineTo(topLeft.x + gap, topLeft.y + digitHeight)
                path.close()
            }
            'E' -> {
                path.moveTo(topLeft.x, topLeft.y + digitHeight - gap)
                path.lineTo(topLeft.x + t, topLeft.y + digitHeight - gap - t)
                path.lineTo(topLeft.x + t + skew * 0.5f, topLeft.y + halfH + gap / 2f + t * 0.5f)
                path.lineTo(topLeft.x + skew * 0.5f, topLeft.y + halfH + gap / 2f)
                path.close()
            }
            'F' -> {
                path.moveTo(topLeft.x + skew, topLeft.y + gap)
                path.lineTo(topLeft.x + t + skew, topLeft.y + gap + t)
                path.lineTo(topLeft.x + t + skew * 0.5f, topLeft.y + halfH - gap / 2f - t * 0.5f)
                path.lineTo(topLeft.x + skew * 0.5f, topLeft.y + halfH - gap / 2f)
                path.close()
            }
            'G' -> {
                path.moveTo(topLeft.x + gap + t / 2f + skew * 0.5f, topLeft.y + halfH - t / 2f)
                path.lineTo(topLeft.x + digitWidth - gap - t / 2f + skew * 0.5f, topLeft.y + halfH - t / 2f)
                path.lineTo(topLeft.x + digitWidth - gap + skew * 0.5f, topLeft.y + halfH)
                path.lineTo(topLeft.x + digitWidth - gap - t / 2f + skew * 0.5f, topLeft.y + halfH + t / 2f)
                path.lineTo(topLeft.x + gap + t / 2f + skew * 0.5f, topLeft.y + halfH + t / 2f)
                path.lineTo(topLeft.x + gap + skew * 0.5f, topLeft.y + halfH)
                path.close()
            }
        }
        drawPath(path = path, color = color)
    }

    listOf('A', 'B', 'C', 'D', 'E', 'F', 'G').forEach { drawSeg(it) }
}

/**
 * HONDA モトコンポ (NC50) スピードメーター 完全再現クロック
 */
@Composable
fun SpeedometerClock(
    uiState: ClockUiState,
    userSettings: UserSettings,
    modifier: Modifier = Modifier
) {
    val condensedBoldTypeface = remember {
        Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    val textPaintWhite = remember {
        Paint().apply {
            color = DialTextWhite.toArgb()
            textSize = 72f
            typeface = condensedBoldTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val textPaintRed = remember {
        Paint().apply {
            color = DialTextRed.toArgb()
            textSize = 72f
            typeface = condensedBoldTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val lcdTextSubLeft = remember {
        Paint().apply {
            color = LcdTextDark.toArgb()
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
    }

    val lcdTextSubRight = remember {
        Paint().apply {
            color = LcdTextDark.toArgb()
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. アウターケース (HONDAモトコンポ 重厚スクエア樹脂ボディ 1.090f 比率)
            val outerWidth = minOf(width * 0.90f, height * 0.84f)
            val outerHeight = outerWidth * 1.090f
            val center = Offset(
                width / 2f + uiState.burnInOffset.offsetX,
                (height / 2f - outerHeight * 0.01f) + uiState.burnInOffset.offsetY
            )

            val outerLeft = center.x - outerWidth / 2f
            val outerTop = center.y - outerHeight / 2f
            val outerCornerR = outerWidth * 0.14f

            // ① ケース上面ハイライト
            drawRoundRect(
                color = BezelTopHighlight,
                topLeft = Offset(outerLeft, outerTop),
                size = Size(outerWidth, outerHeight),
                cornerRadius = CornerRadius(outerCornerR, outerCornerR)
            )

            // ② ケースグラデーション本体
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(BezelBodyStart, BezelBodyEnd),
                    startY = outerTop + 4f,
                    endY = outerTop + outerHeight
                ),
                topLeft = Offset(outerLeft + 2f, outerTop + 2f),
                size = Size(outerWidth - 4f, outerHeight - 4f),
                cornerRadius = CornerRadius(outerCornerR * 0.98f, outerCornerR * 0.98f)
            )

            // ③ 3D 傾斜ベゼル内壁 (文字盤と液晶を包み込む広大リム)
            val wallDepth = outerWidth * 0.055f
            val wallLeft = outerLeft + wallDepth
            val wallTop = outerTop + wallDepth
            val wallWidth = outerWidth - wallDepth * 2f
            val wallHeight = outerHeight - wallDepth * 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(BezelWallDark, BezelWallLight),
                    startY = wallTop,
                    endY = wallTop + wallHeight
                ),
                topLeft = Offset(wallLeft, wallTop),
                size = Size(wallWidth, wallHeight),
                cornerRadius = CornerRadius(outerCornerR * 0.82f, outerCornerR * 0.82f)
            )


            // 2. メイン文字盤 (円形 ラウンドダイアル)
            val dialRadius = outerWidth * 0.420f
            val dialCenter = Offset(
                center.x,
                center.y - outerHeight * 0.035f
            )

            // クリッピング用パスに円形を設定
            val clipPath = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(
                    dialCenter.x - dialRadius,
                    dialCenter.y - dialRadius,
                    dialCenter.x + dialRadius,
                    dialCenter.y + dialRadius
                ))
            }

            // 文字盤背景 (ダークグリーン円形)
            drawCircle(
                color = Color(0xFF0D1A0D),
                radius = dialRadius,
                center = dialCenter
            )

            // 3. レーダー風グリッド: 同心円 + 放射線 (円形クリップ内のみ描画)
            drawContext.canvas.save()
            drawContext.canvas.clipPath(clipPath)

            // 同心円 (3本)
            val concCircles = listOf(0.28f, 0.52f, 0.76f)
            for (fraction in concCircles) {
                drawCircle(
                    color = GridLineGreen.copy(alpha = 0.55f),
                    radius = dialRadius * fraction,
                    center = dialCenter,
                    style = Stroke(width = 1.8f)
                )
            }

            // 放射線 (12本: 30度刻み)
            for (i in 0 until 12) {
                val angleDeg = i * 30f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                drawLine(
                    color = GridLineGreen.copy(alpha = 0.45f),
                    start = dialCenter,
                    end = Offset(
                        dialCenter.x + (dialRadius * Math.cos(angleRad)).toFloat(),
                        dialCenter.y + (dialRadius * Math.sin(angleRad)).toFloat()
                    ),
                    strokeWidth = 1.8f
                )
            }
            drawContext.canvas.restore()

            // 4. 赤帯 (モトコンポ 速度警告レッドゾーン): 外周アーチ目盛り帯 (8〜12時 = 9〜12クロック)
            val redArcRadius = dialRadius * 0.88f
            val redArcStrokeW = dialRadius * 0.090f
            // 9時(-180°)から12時(-90°)まで: sweepAngle=90
            drawArc(
                color = DialTextRed.copy(alpha = 0.92f),
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(dialCenter.x - redArcRadius, dialCenter.y - redArcRadius),
                size = Size(redArcRadius * 2f, redArcRadius * 2f),
                style = Stroke(width = redArcStrokeW, cap = StrokeCap.Butt)
            )
            // 赤帯の細かい刻み目 (9〜12時方向: 9本)
            for (i in 0..8) {
                val angleDeg = 180f + i * 10f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val innerR = redArcRadius - redArcStrokeW / 2f
                val outerR = redArcRadius + redArcStrokeW / 2f
                drawLine(
                    color = Color(0xFF600000).copy(alpha = 0.7f),
                    start = Offset(
                        dialCenter.x + (innerR * Math.cos(angleRad)).toFloat(),
                        dialCenter.y + (innerR * Math.sin(angleRad)).toFloat()
                    ),
                    end = Offset(
                        dialCenter.x + (outerR * Math.cos(angleRad)).toFloat(),
                        dialCenter.y + (outerR * Math.sin(angleRad)).toFloat()
                    ),
                    strokeWidth = 2.5f
                )
            }

            // 5. 放射状 刻み目 (円周上: Minute Ticks 60個)
            for (i in 0 until 60) {
                val angleDeg = i * 6f - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajor = i % 5 == 0
                val outerR = dialRadius * 0.97f
                val innerR = if (isMajor) dialRadius * 0.84f else dialRadius * 0.90f
                val tWidth = if (isMajor) 3.5f else 1.8f
                // 9〜12時の目盛りは赤帯ゾーンに隠れるのでスキップ
                val skipRed = i in 46..59 || i == 0
                val tickColor = if (!skipRed) DialTextWhite
                    else DialTextWhite.copy(alpha = 0f)

                drawLine(
                    color = tickColor,
                    start = Offset(
                        (dialCenter.x + innerR * Math.cos(angleRad)).toFloat(),
                        (dialCenter.y + innerR * Math.sin(angleRad)).toFloat()
                    ),
                    end = Offset(
                        (dialCenter.x + outerR * Math.cos(angleRad)).toFloat(),
                        (dialCenter.y + outerR * Math.sin(angleRad)).toFloat()
                    ),
                    strokeWidth = tWidth
                )
            }

            // 深いインナーシャドウ（円形）
            for (s in 0 until 6) {
                val offset = s * 2.5f
                val alpha = (1f - s.toFloat() / 6) * 0.30f
                drawCircle(
                    color = Color.Black.copy(alpha = alpha),
                    radius = dialRadius - offset,
                    center = dialCenter,
                    style = Stroke(width = 3f)
                )
            }

            // 6. 直立文字盤数字 (1〜12) 円周上に均等配置
            val numRadius = dialRadius * 0.73f
            val fontSize = dialRadius * 0.19f
            textPaintWhite.textSize = fontSize
            textPaintRed.textSize = fontSize

            for (i in 1..12) {
                val angleDeg = i * 30f - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val nx = dialCenter.x + (numRadius * Math.cos(angleRad)).toFloat()
                val ny = dialCenter.y + (numRadius * Math.sin(angleRad)).toFloat()
                val isRed = i in 9..12
                val paint = if (isRed) textPaintRed else textPaintWhite
                val fontMetrics = paint.fontMetrics
                val baselineY = ny - (fontMetrics.ascent + fontMetrics.descent) / 2f
                drawContext.canvas.nativeCanvas.drawText(i.toString(), nx, baselineY, paint)
            }


            // 7. ネジ 2個 (3D リアル立体ビス)
            val screwRadius = dialRadius * 0.048f
            val screwY = dialCenter.y + dialRadius * 0.30f
            val screwX1 = dialCenter.x - dialRadius * 0.28f
            val screwX2 = dialCenter.x + dialRadius * 0.28f

            listOf(screwX1, screwX2).forEach { sx ->
                drawCircle(color = ScrewHoleBg, radius = screwRadius * 1.25f, center = Offset(sx, screwY))
                drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = screwRadius * 1.15f, center = Offset(sx + 2f, screwY + 2f))

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ScrewHeadStart, ScrewHeadEnd),
                        center = Offset(sx - screwRadius * 0.25f, screwY - screwRadius * 0.25f),
                        radius = screwRadius
                    ),
                    radius = screwRadius,
                    center = Offset(sx, screwY)
                )

                val sLen = screwRadius * 0.55f
                drawLine(color = ScrewSlotHighlight, start = Offset(sx - sLen + 1f, screwY - sLen + 1f), end = Offset(sx + sLen + 1f, screwY + sLen + 1f), strokeWidth = 5f)
                drawLine(color = ScrewSlotHighlight, start = Offset(sx - sLen + 1f, screwY + sLen + 1f), end = Offset(sx + sLen + 1f, screwY - sLen + 1f), strokeWidth = 5f)
                drawLine(color = ScrewSlotShadow, start = Offset(sx - sLen, screwY - sLen), end = Offset(sx + sLen, screwY + sLen), strokeWidth = 4f)
                drawLine(color = ScrewSlotShadow, start = Offset(sx - sLen, screwY + sLen), end = Offset(sx + sLen, screwY - sLen), strokeWidth = 4f)
            }

            // 8. HONDA モトコンポ純正 マスタードイエロー 3D アナログ針描画
            val hourHandLength = dialRadius * 0.55f
            val minuteHandLength = dialRadius * 0.78f
            val shadowOffset = Offset(8f, 8f)

            // --- 時針 (Hour Hand) ---
            rotate(degrees = uiState.hourAngleDegree - 90f, pivot = dialCenter) {
                val L = hourHandLength
                val W = dialRadius * 0.062f
                val T = dialRadius * 0.035f

                val shadowPath = Path().apply {
                    moveTo(dialCenter.x - T + shadowOffset.x, dialCenter.y - W + shadowOffset.y)
                    lineTo(dialCenter.x + L - 16f + shadowOffset.x, dialCenter.y - W * 0.35f + shadowOffset.y)
                    lineTo(dialCenter.x + L + shadowOffset.x, dialCenter.y + shadowOffset.y)
                    lineTo(dialCenter.x + L - 16f + shadowOffset.x, dialCenter.y + W * 0.35f + shadowOffset.y)
                    lineTo(dialCenter.x - T + shadowOffset.x, dialCenter.y + W + shadowOffset.y)
                    close()
                }
                drawPath(path = shadowPath, color = HandDropShadow)

                val upperPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y - W)
                    lineTo(dialCenter.x + L - 16f, dialCenter.y - W * 0.30f)
                    lineTo(dialCenter.x + L, dialCenter.y)
                    lineTo(dialCenter.x - T, dialCenter.y)
                    close()
                }
                drawPath(
                    brush = Brush.horizontalGradient(listOf(HandYellowHighlight, HandYellowMain)),
                    path = upperPath
                )

                val lowerPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y)
                    lineTo(dialCenter.x + L, dialCenter.y)
                    lineTo(dialCenter.x + L - 16f, dialCenter.y + W * 0.30f)
                    lineTo(dialCenter.x - T, dialCenter.y + W)
                    close()
                }
                drawPath(
                    brush = Brush.horizontalGradient(listOf(HandYellowShadow, HandYellowDarkShadow)),
                    path = lowerPath
                )

                drawLine(
                    color = InnerBezelFrame.copy(alpha = 0.6f),
                    start = Offset(dialCenter.x - T, dialCenter.y),
                    end = Offset(dialCenter.x + L, dialCenter.y),
                    strokeWidth = 2.0f
                )

                val fullPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y - W)
                    lineTo(dialCenter.x + L - 16f, dialCenter.y - W * 0.30f)
                    lineTo(dialCenter.x + L, dialCenter.y)
                    lineTo(dialCenter.x + L - 16f, dialCenter.y + W * 0.30f)
                    lineTo(dialCenter.x - T, dialCenter.y + W)
                    close()
                }
                drawPath(path = fullPath, color = InnerBezelFrame, style = Stroke(2.0f))
            }

            // --- 分針 (Minute Hand) ---
            rotate(degrees = uiState.minuteAngleDegree - 90f, pivot = dialCenter) {
                val L = minuteHandLength
                val W = dialRadius * 0.050f
                val T = dialRadius * 0.035f

                val shadowPath = Path().apply {
                    moveTo(dialCenter.x - T + shadowOffset.x, dialCenter.y - W + shadowOffset.y)
                    lineTo(dialCenter.x + L - 16f + shadowOffset.x, dialCenter.y - W * 0.35f + shadowOffset.y)
                    lineTo(dialCenter.x + L + shadowOffset.x, dialCenter.y + shadowOffset.y)
                    lineTo(dialCenter.x + L - 16f + shadowOffset.x, dialCenter.y + W * 0.35f + shadowOffset.y)
                    lineTo(dialCenter.x - T + shadowOffset.x, dialCenter.y + W + shadowOffset.y)
                    close()
                }
                drawPath(path = shadowPath, color = HandDropShadow)

                val upperPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y - W)
                    lineTo(dialCenter.x + L - 16f, dialCenter.y - W * 0.30f)
                    lineTo(dialCenter.x + L, dialCenter.y)
                    lineTo(dialCenter.x - T, dialCenter.y)
                    close()
                }
                drawPath(
                    brush = Brush.horizontalGradient(listOf(HandYellowHighlight, HandYellowMain)),
                    path = upperPath
                )

                val lowerPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y)
                    lineTo(dialCenter.x + L, dialCenter.y)
                    lineTo(dialCenter.x + L - 16f, dialCenter.y + W * 0.30f)
                    lineTo(dialCenter.x - T, dialCenter.y + W)
                    close()
                }
                drawPath(
                    brush = Brush.horizontalGradient(listOf(HandYellowShadow, HandYellowDarkShadow)),
                    path = lowerPath
                )

                drawLine(
                    color = InnerBezelFrame.copy(alpha = 0.6f),
                    start = Offset(dialCenter.x - T, dialCenter.y),
                    end = Offset(dialCenter.x + L, dialCenter.y),
                    strokeWidth = 2.0f
                )

                val fullPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y - W)
                    lineTo(dialCenter.x + L - 16f, dialCenter.y - W * 0.30f)
                    lineTo(dialCenter.x + L, dialCenter.y)
                    lineTo(dialCenter.x + L - 16f, dialCenter.y + W * 0.30f)
                    lineTo(dialCenter.x - T, dialCenter.y + W)
                    close()
                }
                drawPath(path = fullPath, color = InnerBezelFrame, style = Stroke(2.0f))
            }

            // 9. 3D センターハブキャップ (2段ドーム構造)
            val capR1 = dialRadius * 0.088f
            val capR2 = capR1 * 0.50f

            drawCircle(color = HandDropShadow, radius = capR1 * 1.15f, center = Offset(dialCenter.x + 6f, dialCenter.y + 6f))

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(HandYellowHighlight, HandYellowMain, HandYellowShadow),
                    center = Offset(dialCenter.x - capR1 * 0.3f, dialCenter.y - capR1 * 0.3f),
                    radius = capR1 * 1.2f
                ),
                radius = capR1,
                center = dialCenter
            )
            drawCircle(color = InnerBezelFrame, radius = capR1, center = dialCenter, style = Stroke(2.5f))

            drawCircle(color = InnerBezelFrame.copy(alpha = 0.40f), radius = capR2 + 2f, center = Offset(dialCenter.x + 1.5f, dialCenter.y + 1.5f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(HandYellowHighlight, HandYellowMain, HandYellowShadow),
                    center = Offset(dialCenter.x - capR2 * 0.3f, dialCenter.y - capR2 * 0.3f),
                    radius = capR2 * 1.2f
                ),
                radius = capR2,
                center = dialCenter
            )
            drawCircle(color = InnerBezelFrame, radius = capR2, center = dialCenter, style = Stroke(1.8f))

            // 10. ベゼル最下部: オドメーター風 7セグTN液晶パネル
            val lcdWidth = dialRadius * 1.40f
            val lcdHeight = lcdWidth * 0.220f
            val lcdTop = dialCenter.y + dialRadius + outerHeight * 0.012f
            val lcdTopLeft = Offset(
                center.x - lcdWidth / 2f,
                lcdTop
            )

            // ① 液晶成型穴 (インセットシャドウ)
            drawRoundRect(
                color = LcdFrameShadow,
                topLeft = Offset(lcdTopLeft.x - 6f, lcdTopLeft.y - 6f),
                size = Size(lcdWidth + 12f, lcdHeight + 12f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.85f),
                topLeft = Offset(lcdTopLeft.x - 3f, lcdTopLeft.y - 3f),
                size = Size(lcdWidth + 6f, lcdHeight + 6f),
                cornerRadius = CornerRadius(9f, 9f)
            )

            // ② TN液晶ガラス基板 (レトロオリーブセピア)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(LcdGlassStart, LcdGlassEnd),
                    startY = lcdTopLeft.y,
                    endY = lcdTopLeft.y + lcdHeight
                ),
                topLeft = lcdTopLeft,
                size = Size(lcdWidth, lcdHeight),
                cornerRadius = CornerRadius(7f, 7f)
            )

            // ③ 液晶反射ハイライト
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    start = Offset(lcdTopLeft.x, lcdTopLeft.y),
                    end = Offset(lcdTopLeft.x + lcdWidth * 0.5f, lcdTopLeft.y + lcdHeight)
                ),
                topLeft = lcdTopLeft,
                size = Size(lcdWidth, lcdHeight),
                cornerRadius = CornerRadius(7f, 7f)
            )

            val monthStr = SimpleDateFormat("MMM", Locale.US).format(uiState.currentTime).uppercase()
            val dayStr = SimpleDateFormat("dd", Locale.US).format(uiState.currentTime)
            val timeDigits = uiState.currentTime.toTimeString()
            val amPmStr = uiState.currentTime.toAmPmString()

            val weatherStr = if (userSettings.showWeather) {
                "Sunny, ${uiState.weatherInfo.temperatureCelsius.toFormattedTemperature(userSettings.temperatureUnit)}"
            } else ""

            val batteryStr = if (userSettings.showBattery) {
                "⚡${uiState.batteryState.levelPercentage.toPercentString()}"
            } else ""

            // --- TN液晶テキスト描画 ---

            // ① 左ブロック: 月 (上) / 日 (下) & AM/PM
            val col1X = lcdTopLeft.x + lcdWidth * 0.035f
            lcdTextSubLeft.textSize = lcdHeight * 0.23f
            drawContext.canvas.nativeCanvas.drawText(monthStr, col1X, lcdTopLeft.y + lcdHeight * 0.36f, lcdTextSubLeft)
            drawContext.canvas.nativeCanvas.drawText(dayStr, col1X, lcdTopLeft.y + lcdHeight * 0.80f, lcdTextSubLeft)

            val amPmPaint = Paint(lcdTextSubLeft).apply { textSize = lcdHeight * 0.17f; textAlign = Paint.Align.LEFT }
            drawContext.canvas.nativeCanvas.drawText(amPmStr, col1X + lcdWidth * 0.11f, lcdTopLeft.y + lcdHeight * 0.80f, amPmPaint)

            // ② 中央ブロック: 極太・大画面 7セグメントデジタル時刻描画
            val segW = lcdHeight * 0.24f
            val segH = lcdHeight * 0.68f
            val segY = lcdTopLeft.y + lcdHeight * 0.16f
            val totalSegWidth = segW * 4.48f + segW * 0.4f
            val startSegX = center.x - totalSegWidth / 2f + segW * 0.15f

            // ゴースト 88:88
            var currentGhostX = startSegX
            for (c in "88:88") {
                if (c == ':') {
                    val dotR = segW * 0.13f
                    drawCircle(color = LcdTextGhost, radius = dotR, center = Offset(currentGhostX + segW * 0.2f, segY + segH * 0.3f))
                    drawCircle(color = LcdTextGhost, radius = dotR, center = Offset(currentGhostX + segW * 0.2f, segY + segH * 0.7f))
                    currentGhostX += segW * 0.4f
                } else {
                    draw7SegmentDigit(
                        digit = '8',
                        topLeft = Offset(currentGhostX, segY),
                        digitWidth = segW,
                        digitHeight = segH,
                        color = LcdTextGhost,
                        thickness = segW * 0.26f
                    )
                    currentGhostX += segW * 1.12f
                }
            }

            // 点灯時刻
            var currentSegX = startSegX
            for (c in timeDigits) {
                if (c == ':') {
                    val dotR = segW * 0.13f
                    drawCircle(color = LcdTextDark, radius = dotR, center = Offset(currentSegX + segW * 0.2f, segY + segH * 0.3f))
                    drawCircle(color = LcdTextDark, radius = dotR, center = Offset(currentSegX + segW * 0.2f, segY + segH * 0.7f))
                    currentSegX += segW * 0.4f
                } else {
                    draw7SegmentDigit(
                        digit = c,
                        topLeft = Offset(currentSegX, segY),
                        digitWidth = segW,
                        digitHeight = segH,
                        color = LcdTextDark,
                        thickness = segW * 0.26f
                    )
                    currentSegX += segW * 1.12f
                }
            }

            // ③ 右ブロック: 天気 (上) / バッテリー (下)
            val col3X = lcdTopLeft.x + lcdWidth * 0.965f
            lcdTextSubRight.textSize = lcdHeight * 0.19f
            drawContext.canvas.nativeCanvas.drawText(weatherStr, col3X, lcdTopLeft.y + lcdHeight * 0.36f, lcdTextSubRight)
            drawContext.canvas.nativeCanvas.drawText(batteryStr, col3X, lcdTopLeft.y + lcdHeight * 0.80f, lcdTextSubRight)
        }
    }
}

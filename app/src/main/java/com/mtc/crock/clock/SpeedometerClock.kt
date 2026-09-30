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
import com.mtc.crock.util.toFormattedIntTemperature
import com.mtc.crock.util.toPercentString
import com.mtc.crock.util.toTimeString
import java.text.SimpleDateFormat
import java.util.Locale

// HONDA モトコンポ (NC50) スピードメーター 完全再現トーン＆カラー
private val BezelTopHighlight = Color(0xFF424448)
private val BezelBodyStart = Color(0xFF282A2E)
private val BezelBodyEnd = Color(0xFF141517)
private val BezelWallDark = Color(0xFF060708)
private val BezelWallLight = Color(0xFF1F2124)
private val InnerBezelFrame = Color(0xFF0A0B0C)

private val DialBgColor = Color(0xFF151618)
// ホンダ純正 モトコンポ エメラルドグリーン格子
private val GridLineGreen = Color(0xFF3EB459).copy(alpha = 0.82f)

private val DialTextWhite = Color(0xFFFFFFFF)
private val DialTextRed = Color(0xFFE22D2D) // モトコンポ 30km/h 速度警告レッド
private val RedArcColor = Color(0xFFB81E1E)

// HONDA 純正マスタードイエロー 3D メーター針
private val HandYellowHighlight = Color(0xFFFDC52F)
private val HandYellowMain = Color(0xFFEAA615)
private val HandYellowShadow = Color(0xFFC78408)
private val HandYellowDarkShadow = Color(0xFF8F5A02)
private val HandDropShadow = Color(0xFF000000).copy(alpha = 0.60f)

// 3D ビスカラー (添付画像通りの質感とスリットコントラスト)
private val ScrewHoleBg = Color(0xFF040506)
private val ScrewHeadStart = Color(0xFF4A4E54)
private val ScrewHeadEnd = Color(0xFF1A1C1F)
private val ScrewSlotShadow = Color(0xFF020203)
private val ScrewSlotHighlight = Color(0xFF6E737B)

// TN液晶カラー (オドメーター風 レトロオリーブセピア)
private val LcdGlassStart = Color(0xFF98A693)
private val LcdGlassEnd = Color(0xFFA5B2A0)
private val LcdFrameShadow = Color(0xFF060708)
private val LcdTextDark = Color(0xFF141A12)
private val LcdTextGhost = Color(0x18141A12)

/**
 * リアル7セグメント液晶文字描画エンジン (角張りデジタルバー)
 */
private fun DrawScope.draw7SegmentDigit(
    digit: Char,
    topLeft: Offset,
    digitWidth: Float,
    digitHeight: Float,
    color: Color,
    thickness: Float = digitWidth * 0.25f,
    skew: Float = digitWidth * 0.05f
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
    // 添付画像のドッシリとした太字サンセリフ書体
    val boldTypeface = remember {
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    val textPaintWhite = remember {
        Paint().apply {
            color = DialTextWhite.toArgb()
            textSize = 72f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val textPaintRed = remember {
        Paint().apply {
            color = DialTextRed.toArgb()
            textSize = 72f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val lcdTextSubLeft = remember {
        Paint().apply {
            color = LcdTextDark.toArgb()
            textSize = 20f
            typeface = boldTypeface
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
    }

    val lcdTextSubRight = remember {
        Paint().apply {
            color = LcdTextDark.toArgb()
            textSize = 20f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. アウターケース (HONDAモトコンポ 重厚スクエア樹脂ボディ)
            val outerWidth = minOf(width * 0.90f, height * 0.86f)
            val outerHeight = outerWidth * 1.085f
            val center = Offset(
                width / 2f + uiState.burnInOffset.offsetX,
                (height / 2f - outerHeight * 0.008f) + uiState.burnInOffset.offsetY
            )

            val outerLeft = center.x - outerWidth / 2f
            val outerTop = center.y - outerHeight / 2f
            val outerCornerR = outerWidth * 0.16f

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
                topLeft = Offset(outerLeft + 2.5f, outerTop + 2.5f),
                size = Size(outerWidth - 5f, outerHeight - 5f),
                cornerRadius = CornerRadius(outerCornerR * 0.98f, outerCornerR * 0.98f)
            )

            // ③ 3D 傾斜ベゼル内壁 (文字盤と液晶を包み込むリム)
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

            // 2. メイン文字盤 (正方形領域)
            val dialPadding = outerWidth * 0.078f
            val dialSize = outerWidth - dialPadding * 2f
            val dialLeft = outerLeft + dialPadding
            val dialTop = outerTop + outerWidth * 0.062f
            val dialCenter = Offset(dialLeft + dialSize / 2f, dialTop + dialSize / 2f)
            val dialCornerR = outerCornerR * 0.46f

            // 文字盤背景 (スクエア)
            drawRoundRect(
                color = DialBgColor,
                topLeft = Offset(dialLeft, dialTop),
                size = Size(dialSize, dialSize),
                cornerRadius = CornerRadius(dialCornerR, dialCornerR)
            )

            // 3. HONDA純正 エメラルドグリーン格子模様 (縦5本×横5本 = 完全正方形 6等分)
            val gridCols = 6
            val gridStep = dialSize / gridCols
            for (i in 1 until gridCols) {
                val gx = dialLeft + gridStep * i
                drawLine(
                    color = GridLineGreen,
                    start = Offset(gx, dialTop + 5f),
                    end = Offset(gx, dialTop + dialSize - 5f),
                    strokeWidth = 2.6f
                )
            }

            for (i in 1 until gridCols) {
                val gy = dialTop + gridStep * i
                drawLine(
                    color = GridLineGreen,
                    start = Offset(dialLeft + 5f, gy),
                    end = Offset(dialLeft + dialSize - 5f, gy),
                    strokeWidth = 2.6f
                )
            }

            // 深いインナーシャドウ
            val shadowSteps = 6
            for (s in 0 until shadowSteps) {
                val offset = s * 2.5f
                val alpha = (1f - s.toFloat() / shadowSteps) * 0.35f
                drawRoundRect(
                    color = Color.Black.copy(alpha = alpha),
                    topLeft = Offset(dialLeft + offset, dialTop + offset),
                    size = Size(dialSize - offset * 2f, dialSize - offset * 2f),
                    cornerRadius = CornerRadius(dialCornerR - offset * 0.5f, dialCornerR - offset * 0.5f),
                    style = Stroke(width = 3f)
                )
            }

            // 4. 赤帯 (モトコンポ 30km/h 速度警告レッドアーチ: 9時→12時の90°扇形アーチ)
            // 目標デザイン厳密準拠：数字9〜12の内側、グリッド交点(X2,Y2)を通過し、数字とは一切重ならない独立した扇形
            val arcRadius = gridStep * 1.48f
            val arcStrokeWidth = gridStep * 0.74f
            drawArc(
                color = RedArcColor,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(dialCenter.x - arcRadius, dialCenter.y - arcRadius),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Butt)
            )

            // 5. 白い刻み (Minute Ticks: 外端は四角枠に沿って放射状に配置)
            val boxHalf = dialSize * 0.472f
            for (i in 0 until 60) {
                val angleDeg = i * 6f - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val cosA = Math.cos(angleRad)
                val sinA = Math.sin(angleRad)

                val absCos = Math.abs(cosA)
                val absSin = Math.abs(sinA)

                val scale = minOf(
                    if (absCos > 1e-4) boxHalf / absCos else Double.MAX_VALUE,
                    if (absSin > 1e-4) boxHalf / absSin else Double.MAX_VALUE
                ).toFloat()

                val endX = dialCenter.x + scale * cosA.toFloat()
                val endY = dialCenter.y + scale * sinA.toFloat()

                val isMajor = i % 5 == 0
                val tLen = if (isMajor) dialSize * 0.038f else dialSize * 0.020f
                val tWidth = if (isMajor) 3.5f else 1.8f

                val startX = endX - tLen * cosA.toFloat()
                val startY = endY - tLen * sinA.toFloat()

                drawLine(
                    color = DialTextWhite,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tWidth
                )
            }

            // 6. 直立文字盤数字 (1〜12) の配置: 外周グリッドマスに配置し、赤帯と完全に分離
            val fontSize = dialSize * 0.145f
            textPaintWhite.textSize = fontSize
            textPaintRed.textSize = fontSize

            val numPositions = mapOf(
                12 to Offset(dialCenter.x, dialTop + gridStep * 0.48f),
                1  to Offset(dialLeft + gridStep * 4.35f, dialTop + gridStep * 0.50f),
                2  to Offset(dialLeft + gridStep * 5.40f, dialTop + gridStep * 1.65f),
                3  to Offset(dialLeft + dialSize * 0.905f, dialCenter.y),
                4  to Offset(dialLeft + gridStep * 5.40f, dialTop + gridStep * 4.35f),
                5  to Offset(dialLeft + gridStep * 4.35f, dialTop + gridStep * 5.52f),
                6  to Offset(dialCenter.x, dialTop + gridStep * 5.52f),
                7  to Offset(dialLeft + gridStep * 1.65f, dialTop + gridStep * 5.52f),
                8  to Offset(dialLeft + gridStep * 0.60f, dialTop + gridStep * 4.35f),
                9  to Offset(dialLeft + dialSize * 0.095f, dialCenter.y),
                10 to Offset(dialLeft + gridStep * 0.56f, dialTop + gridStep * 1.65f),
                11 to Offset(dialLeft + gridStep * 1.65f, dialTop + gridStep * 0.50f)
            )

            for (i in 1..12) {
                val pos = numPositions[i] ?: continue
                val isRed = i in 9..12
                val paint = if (isRed) textPaintRed else textPaintWhite
                val fontMetrics = paint.fontMetrics
                val baselineY = pos.y - (fontMetrics.ascent + fontMetrics.descent) / 2f

                drawContext.canvas.nativeCanvas.drawText(
                    i.toString(),
                    pos.x,
                    baselineY,
                    paint
                )
            }

            // 7. ネジ 2個 (添付画像：下から2本目の水平グリッド線上、クッキリした立体感)
            val screwRadius = dialSize * 0.046f
            val screwY = dialTop + gridStep * 4.0f
            val screwX1 = dialLeft + gridStep * 1.88f
            val screwX2 = dialLeft + gridStep * 4.12f

            listOf(screwX1 to 6f, screwX2 to -4f).forEach { (sx, rotDeg) ->
                drawCircle(color = ScrewHoleBg, radius = screwRadius * 1.25f, center = Offset(sx, screwY))
                drawCircle(color = Color.Black.copy(alpha = 0.55f), radius = screwRadius * 1.15f, center = Offset(sx + 1.5f, screwY + 1.5f))

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ScrewHeadStart, ScrewHeadEnd),
                        center = Offset(sx - screwRadius * 0.25f, screwY - screwRadius * 0.25f),
                        radius = screwRadius
                    ),
                    radius = screwRadius,
                    center = Offset(sx, screwY)
                )

                // プラス溝 (自然なわずかな回転角と立体ハイライト)
                rotate(degrees = rotDeg, pivot = Offset(sx, screwY)) {
                    val sLen = screwRadius * 0.54f
                    drawLine(color = ScrewSlotHighlight, start = Offset(sx - sLen + 0.8f, screwY + 0.8f), end = Offset(sx + sLen + 0.8f, screwY + 0.8f), strokeWidth = 3.6f)
                    drawLine(color = ScrewSlotShadow, start = Offset(sx - sLen, screwY), end = Offset(sx + sLen, screwY), strokeWidth = 3.2f)
                    drawLine(color = ScrewSlotHighlight, start = Offset(sx + 0.8f, screwY - sLen + 0.8f), end = Offset(sx + 0.8f, screwY + sLen + 0.8f), strokeWidth = 3.6f)
                    drawLine(color = ScrewSlotShadow, start = Offset(sx, screwY - sLen), end = Offset(sx, screwY + sLen), strokeWidth = 3.2f)
                }
            }

            // 8. HONDA モトコンポ純正 マスタードイエロー 3D アナログ針描画
            val hourHandLength = dialSize * 0.320f
            val minuteHandLength = dialSize * 0.450f
            val shadowOffset = Offset(7f, 7f)

            // --- 時針 (Hour Hand) ---
            rotate(degrees = uiState.hourAngleDegree - 90f, pivot = dialCenter) {
                val L = hourHandLength
                val W = dialSize * 0.088f
                val T = dialSize * 0.045f
                val tipW = dialSize * 0.030f

                val shadowPath = Path().apply {
                    moveTo(dialCenter.x - T + shadowOffset.x, dialCenter.y - W + shadowOffset.y)
                    lineTo(dialCenter.x + L * 0.38f + shadowOffset.x, dialCenter.y - W * 0.42f + shadowOffset.y)
                    lineTo(dialCenter.x + L + shadowOffset.x, dialCenter.y - tipW + shadowOffset.y)
                    lineTo(dialCenter.x + L + shadowOffset.x, dialCenter.y + tipW + shadowOffset.y)
                    lineTo(dialCenter.x + L * 0.38f + shadowOffset.x, dialCenter.y + W * 0.42f + shadowOffset.y)
                    lineTo(dialCenter.x - T + shadowOffset.x, dialCenter.y + W + shadowOffset.y)
                    close()
                }
                drawPath(path = shadowPath, color = HandDropShadow)

                // 上半分 (ハイライト面)
                val upperPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y - W)
                    lineTo(dialCenter.x + L * 0.38f, dialCenter.y - W * 0.42f)
                    lineTo(dialCenter.x + L, dialCenter.y - tipW)
                    lineTo(dialCenter.x + L, dialCenter.y)
                    lineTo(dialCenter.x - T, dialCenter.y)
                    close()
                }
                drawPath(
                    brush = Brush.horizontalGradient(listOf(HandYellowHighlight, HandYellowMain)),
                    path = upperPath
                )

                // 下半分 (シャドウ面)
                val lowerPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y)
                    lineTo(dialCenter.x + L, dialCenter.y)
                    lineTo(dialCenter.x + L, dialCenter.y + tipW)
                    lineTo(dialCenter.x + L * 0.38f, dialCenter.y + W * 0.42f)
                    lineTo(dialCenter.x - T, dialCenter.y + W)
                    close()
                }
                drawPath(
                    brush = Brush.horizontalGradient(listOf(HandYellowShadow, HandYellowDarkShadow)),
                    path = lowerPath
                )

                // センター稜線
                drawLine(
                    color = InnerBezelFrame.copy(alpha = 0.5f),
                    start = Offset(dialCenter.x - T, dialCenter.y),
                    end = Offset(dialCenter.x + L, dialCenter.y),
                    strokeWidth = 1.8f
                )

                // 輪郭線
                val fullPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y - W)
                    lineTo(dialCenter.x + L * 0.38f, dialCenter.y - W * 0.42f)
                    lineTo(dialCenter.x + L, dialCenter.y - tipW)
                    lineTo(dialCenter.x + L, dialCenter.y + tipW)
                    lineTo(dialCenter.x + L * 0.38f, dialCenter.y + W * 0.42f)
                    lineTo(dialCenter.x - T, dialCenter.y + W)
                    close()
                }
                drawPath(path = fullPath, color = InnerBezelFrame, style = Stroke(1.8f))
            }

            // --- 分針 (Minute Hand) ---
            rotate(degrees = uiState.minuteAngleDegree - 90f, pivot = dialCenter) {
                val L = minuteHandLength
                val W = dialSize * 0.076f
                val T = dialSize * 0.045f
                val tipW = dialSize * 0.024f

                val shadowPath = Path().apply {
                    moveTo(dialCenter.x - T + shadowOffset.x, dialCenter.y - W + shadowOffset.y)
                    lineTo(dialCenter.x + L * 0.35f + shadowOffset.x, dialCenter.y - W * 0.42f + shadowOffset.y)
                    lineTo(dialCenter.x + L + shadowOffset.x, dialCenter.y - tipW + shadowOffset.y)
                    lineTo(dialCenter.x + L + shadowOffset.x, dialCenter.y + tipW + shadowOffset.y)
                    lineTo(dialCenter.x + L * 0.35f + shadowOffset.x, dialCenter.y + W * 0.42f + shadowOffset.y)
                    lineTo(dialCenter.x - T + shadowOffset.x, dialCenter.y + W + shadowOffset.y)
                    close()
                }
                drawPath(path = shadowPath, color = HandDropShadow)

                // 上半分 (ハイライト面)
                val upperPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y - W)
                    lineTo(dialCenter.x + L * 0.35f, dialCenter.y - W * 0.42f)
                    lineTo(dialCenter.x + L, dialCenter.y - tipW)
                    lineTo(dialCenter.x + L, dialCenter.y)
                    lineTo(dialCenter.x - T, dialCenter.y)
                    close()
                }
                drawPath(
                    brush = Brush.horizontalGradient(listOf(HandYellowHighlight, HandYellowMain)),
                    path = upperPath
                )

                // 下半分 (シャドウ面)
                val lowerPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y)
                    lineTo(dialCenter.x + L, dialCenter.y)
                    lineTo(dialCenter.x + L, dialCenter.y + tipW)
                    lineTo(dialCenter.x + L * 0.35f, dialCenter.y + W * 0.42f)
                    lineTo(dialCenter.x - T, dialCenter.y + W)
                    close()
                }
                drawPath(
                    brush = Brush.horizontalGradient(listOf(HandYellowShadow, HandYellowDarkShadow)),
                    path = lowerPath
                )

                // センター稜線
                drawLine(
                    color = InnerBezelFrame.copy(alpha = 0.5f),
                    start = Offset(dialCenter.x - T, dialCenter.y),
                    end = Offset(dialCenter.x + L, dialCenter.y),
                    strokeWidth = 1.8f
                )

                // 輪郭線
                val fullPath = Path().apply {
                    moveTo(dialCenter.x - T, dialCenter.y - W)
                    lineTo(dialCenter.x + L * 0.35f, dialCenter.y - W * 0.42f)
                    lineTo(dialCenter.x + L, dialCenter.y - tipW)
                    lineTo(dialCenter.x + L, dialCenter.y + tipW)
                    lineTo(dialCenter.x + L * 0.35f, dialCenter.y + W * 0.42f)
                    lineTo(dialCenter.x - T, dialCenter.y + W)
                    close()
                }
                drawPath(path = fullPath, color = InnerBezelFrame, style = Stroke(1.8f))
            }

            // 9. 3D センターハブキャップ & 下部爪パーツ (添付画像通りの斜め下2つの爪ウィング造形)
            val capR1 = dialSize * 0.082f

            // ① ハブ下左右の2つの斜め爪ウィング
            val leftWingPath = Path().apply {
                moveTo(dialCenter.x - capR1 * 0.70f, dialCenter.y + capR1 * 0.35f)
                lineTo(dialCenter.x - capR1 * 1.15f, dialCenter.y + capR1 * 0.65f)
                lineTo(dialCenter.x - capR1 * 0.95f, dialCenter.y + capR1 * 0.95f)
                lineTo(dialCenter.x - capR1 * 0.45f, dialCenter.y + capR1 * 0.70f)
                close()
            }
            val rightWingPath = Path().apply {
                moveTo(dialCenter.x + capR1 * 0.70f, dialCenter.y + capR1 * 0.35f)
                lineTo(dialCenter.x + capR1 * 1.15f, dialCenter.y + capR1 * 0.65f)
                lineTo(dialCenter.x + capR1 * 0.95f, dialCenter.y + capR1 * 0.95f)
                lineTo(dialCenter.x + capR1 * 0.45f, dialCenter.y + capR1 * 0.70f)
                close()
            }

            listOf(leftWingPath, rightWingPath).forEach { wPath ->
                val shadowP = Path().apply { addPath(wPath, Offset(4f, 4f)) }
                drawPath(path = shadowP, color = HandDropShadow)
                drawPath(
                    path = wPath,
                    brush = Brush.verticalGradient(
                        listOf(HandYellowMain, HandYellowShadow),
                        startY = dialCenter.y,
                        endY = dialCenter.y + capR1 * 1.1f
                    )
                )
                drawPath(path = wPath, color = InnerBezelFrame, style = Stroke(1.8f))
            }

            // ② 丸型センタードームキャップ
            drawCircle(color = HandDropShadow, radius = capR1 * 1.12f, center = Offset(dialCenter.x + 4f, dialCenter.y + 4f))

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(HandYellowHighlight, HandYellowMain, HandYellowShadow),
                    center = Offset(dialCenter.x - capR1 * 0.3f, dialCenter.y - capR1 * 0.3f),
                    radius = capR1 * 1.1f
                ),
                radius = capR1,
                center = dialCenter
            )
            drawCircle(color = InnerBezelFrame, radius = capR1, center = dialCenter, style = Stroke(2.2f))

            val capR2 = capR1 * 0.52f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(HandYellowHighlight, HandYellowMain),
                    center = Offset(dialCenter.x - capR2 * 0.2f, dialCenter.y - capR2 * 0.2f),
                    radius = capR2 * 1.1f
                ),
                radius = capR2,
                center = dialCenter
            )
            drawCircle(color = InnerBezelFrame.copy(alpha = 0.6f), radius = capR2, center = dialCenter, style = Stroke(1.5f))

            // 10. ベゼル最下部: オドメーター風 7セグTN液晶パネル (添付画像スタイル)
            val lcdWidth = dialSize * 0.68f
            val lcdHeight = lcdWidth * 0.230f
            val lcdTop = dialTop + dialSize + outerHeight * 0.012f
            val lcdTopLeft = Offset(
                center.x - lcdWidth / 2f,
                lcdTop
            )

            // ① 液晶成型穴 (ベゼルインセット段差)
            drawRoundRect(
                color = LcdFrameShadow,
                topLeft = Offset(lcdTopLeft.x - 7f, lcdTopLeft.y - 6f),
                size = Size(lcdWidth + 14f, lcdHeight + 12f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.90f),
                topLeft = Offset(lcdTopLeft.x - 3.5f, lcdTopLeft.y - 3f),
                size = Size(lcdWidth + 7f, lcdHeight + 6f),
                cornerRadius = CornerRadius(10f, 10f)
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
                cornerRadius = CornerRadius(8f, 8f)
            )

            // ③ 液晶反射ハイライト
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                    start = Offset(lcdTopLeft.x, lcdTopLeft.y),
                    end = Offset(lcdTopLeft.x + lcdWidth * 0.45f, lcdTopLeft.y + lcdHeight)
                ),
                topLeft = lcdTopLeft,
                size = Size(lcdWidth, lcdHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // --- TN液晶テキストデータ準備 ---
            val monthStr = SimpleDateFormat("MMM", Locale.US).format(uiState.currentTime).uppercase()
            val dayStr = SimpleDateFormat("dd", Locale.US).format(uiState.currentTime)
            val timeDigits = uiState.currentTime.toTimeString()
            val amPmStr = uiState.currentTime.toAmPmString()

            // 添付画像形式: "Sunny, 72°F" (英語天気名 + カンマ + 温度)
            val weatherStr = if (userSettings.showWeather) {
                val cond = uiState.weatherInfo.conditionTextEn.ifEmpty { "Sunny" }
                val temp = uiState.weatherInfo.temperatureCelsius.toFormattedIntTemperature(userSettings.temperatureUnit)
                "$cond, $temp"
            } else ""

            val batteryPercentStr = if (userSettings.showBattery) {
                uiState.batteryState.levelPercentage.toPercentString()
            } else ""

            // --- TN液晶テキスト描画 ---

            // ① 左ブロック: 月 (上) / 日 (下) & AM/PM (添付画像通りの配置)
            val col1X = lcdTopLeft.x + lcdWidth * 0.040f
            lcdTextSubLeft.textSize = lcdHeight * 0.28f
            drawContext.canvas.nativeCanvas.drawText(monthStr, col1X, lcdTopLeft.y + lcdHeight * 0.38f, lcdTextSubLeft)
            drawContext.canvas.nativeCanvas.drawText(dayStr, col1X, lcdTopLeft.y + lcdHeight * 0.82f, lcdTextSubLeft)

            val amPmPaint = Paint(lcdTextSubLeft).apply { textSize = lcdHeight * 0.20f; textAlign = Paint.Align.LEFT }
            drawContext.canvas.nativeCanvas.drawText(amPmStr, col1X + lcdWidth * 0.145f, lcdTopLeft.y + lcdHeight * 0.38f, amPmPaint)

            // ② 中央ブロック: 7セグメントデジタル時刻描画 (時:分)
            val segW = lcdHeight * 0.235f
            val segH = lcdHeight * 0.68f
            val segY = lcdTopLeft.y + lcdHeight * 0.16f
            val totalSegWidth = segW * 4.45f + segW * 0.40f
            val startSegX = center.x - totalSegWidth / 2f + segW * 0.10f

            // ゴースト 88:88
            var currentGhostX = startSegX
            for (c in "88:88") {
                if (c == ':') {
                    val dotR = segW * 0.12f
                    drawCircle(color = LcdTextGhost, radius = dotR, center = Offset(currentGhostX + segW * 0.20f, segY + segH * 0.32f))
                    drawCircle(color = LcdTextGhost, radius = dotR, center = Offset(currentGhostX + segW * 0.20f, segY + segH * 0.68f))
                    currentGhostX += segW * 0.40f
                } else {
                    draw7SegmentDigit(
                        digit = '8',
                        topLeft = Offset(currentGhostX, segY),
                        digitWidth = segW,
                        digitHeight = segH,
                        color = LcdTextGhost,
                        thickness = segW * 0.24f
                    )
                    currentGhostX += segW * 1.10f
                }
            }

            // 点灯時刻
            var currentSegX = startSegX
            for (c in timeDigits) {
                if (c == ':') {
                    val dotR = segW * 0.12f
                    drawCircle(color = LcdTextDark, radius = dotR, center = Offset(currentSegX + segW * 0.20f, segY + segH * 0.32f))
                    drawCircle(color = LcdTextDark, radius = dotR, center = Offset(currentSegX + segW * 0.20f, segY + segH * 0.68f))
                    currentSegX += segW * 0.40f
                } else {
                    draw7SegmentDigit(
                        digit = c,
                        topLeft = Offset(currentSegX, segY),
                        digitWidth = segW,
                        digitHeight = segH,
                        color = LcdTextDark,
                        thickness = segW * 0.24f
                    )
                    currentSegX += segW * 1.10f
                }
            }

            // ③ 右ブロック: 天気テキスト (上) / ベクター液晶バッテリーアイコン + % (下)
            val col3RightX = lcdTopLeft.x + lcdWidth * 0.960f
            lcdTextSubRight.textSize = lcdHeight * 0.21f
            if (weatherStr.isNotEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(weatherStr, col3RightX, lcdTopLeft.y + lcdHeight * 0.38f, lcdTextSubRight)
            }

            if (batteryPercentStr.isNotEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(batteryPercentStr, col3RightX, lcdTopLeft.y + lcdHeight * 0.82f, lcdTextSubRight)

                // 添付画像通りのベクター液晶バッテリーアイコン描画
                val textWidth = lcdTextSubRight.measureText(batteryPercentStr)
                val battW = lcdHeight * 0.28f
                val battH = lcdHeight * 0.17f
                val battRight = col3RightX - textWidth - 5f
                val battLeft = battRight - battW
                val battTop = lcdTopLeft.y + lcdHeight * 0.82f - battH * 0.95f

                // 外枠
                drawRoundRect(
                    color = LcdTextDark,
                    topLeft = Offset(battLeft, battTop),
                    size = Size(battW, battH),
                    cornerRadius = CornerRadius(2.5f, 2.5f),
                    style = Stroke(width = 1.6f)
                )
                // プラス極突起 (右端中央)
                val tipH = battH * 0.45f
                drawRoundRect(
                    color = LcdTextDark,
                    topLeft = Offset(battRight, battTop + (battH - tipH) / 2f),
                    size = Size(2.2f, tipH),
                    cornerRadius = CornerRadius(1f, 1f)
                )
                // 内部充電バー
                val level = (uiState.batteryState.levelPercentage / 100f).coerceIn(0f, 1f)
                val innerPadding = 2.0f
                val barMaxWidth = battW - innerPadding * 2f
                val barWidth = barMaxWidth * level
                if (barWidth > 0f) {
                    drawRect(
                        color = LcdTextDark,
                        topLeft = Offset(battLeft + innerPadding, battTop + innerPadding),
                        size = Size(barWidth, battH - innerPadding * 2f)
                    )
                }
            }
        }
    }
}

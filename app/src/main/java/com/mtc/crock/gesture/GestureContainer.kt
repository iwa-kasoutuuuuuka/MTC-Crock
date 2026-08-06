package com.mtc.crock.gesture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtc.crock.util.Constants
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * タップ誤動作防止およびマルチジェスチャー (スワイプ, 長押し, ダブルタップ) 処理コンポーネント
 */
@Composable
fun GestureContainer(
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    currentBrightness: Float,
    content: @Composable () -> Unit
) {
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var overlayBrightnessValue by remember { mutableFloatStateOf(currentBrightness) }

    // 輝度変化検出用の累積ドラッグ量
    var totalDragY by remember { mutableFloatStateOf(0f) }

    // 明るさオーバーレイタイマー
    LaunchedEffect(showBrightnessOverlay, overlayBrightnessValue) {
        if (showBrightnessOverlay) {
            delay(Constants.BRIGHTNESS_OVERLAY_TIMEOUT_MS)
            showBrightnessOverlay = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDragEnd = {},
                    onDragCancel = {},
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x
                        val dy = dragAmount.y
                        totalDragY += dy

                        // 横スワイプ判定
                        if (abs(dx) > abs(dy) * 1.8f && abs(dx) > 35f) {
                            if (dx > 0) {
                                onSwipeRight() // 右スワイプ -> 設定画面
                            } else {
                                onSwipeLeft() // 左スワイプ -> 天気画面
                            }
                        }
                        // 縦スワイプ判定 (明るさ調整)
                        else if (abs(dy) > abs(dx) * 1.5f) {
                            val delta = -dy / 500f // 上にスワイプで明るく
                            val newBrightness = (overlayBrightnessValue + delta).coerceIn(0.01f, 1.0f)
                            overlayBrightnessValue = newBrightness
                            onBrightnessChanged(newBrightness)
                            showBrightnessOverlay = true
                        }
                    }
                )
            }
    ) {
        content()

        // 輝度調整時の画面オーバーレイ表示 (0〜100%)
        AnimatedVisibility(
            visible = showBrightnessOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
        ) {
            val percent = (overlayBrightnessValue * 100).toInt()
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "☀️ 画面明るさ: $percent%",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

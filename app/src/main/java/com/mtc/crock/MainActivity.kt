package com.mtc.crock

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mtc.crock.clock.ClockViewModel
import com.mtc.crock.data.SettingsRepository
import com.mtc.crock.theme.BlackBackground
import com.mtc.crock.theme.MTCCrockTheme
import com.mtc.crock.ui.MainScreen

/**
 * MTC-Crock メインアクティビティ
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ClockViewModel by viewModels()
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(applicationContext)

        // 1. 横画面固定
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // 2. スリープ禁止 (常時点灯)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 3. UIのレイアウト設定
        setContent {
            val userSettings by viewModel.userSettings.collectAsState()

            MTCCrockTheme(appTheme = userSettings.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BlackBackground
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        settingsRepository = settingsRepository,
                        onBrightnessChanged = { brightness ->
                            setScreenBrightness(brightness)
                        }
                    )
                }
            }
        }

        // 4. 全画面・イマーシブモード設定 (安全に適用)
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    /**
     * ステータスバーおよびナビゲーションバーを安全かつ確実に非表示にする
     */
    private fun hideSystemUI() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (e: Exception) {
            // 例外発生時フォールバック
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    /**
     * 画面輝度の動的設定 (0.01f ~ 1.0f)
     */
    private fun setScreenBrightness(brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
        window.attributes = layoutParams
    }
}

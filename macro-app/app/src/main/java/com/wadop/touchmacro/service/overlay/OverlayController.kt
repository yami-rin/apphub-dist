package com.wadop.touchmacro.service.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout

/**
 * 録画/再生の操作用オーバーレイを管理する。
 *
 * 設計:
 * - オーバーレイはアイコンボタンだけを並べた小さなパネル。全画面を覆わないため、
 *   配下アプリの通常操作を妨げない（パネル領域外のタッチは配下へ透過する）。
 * - オーバーレイ自身へのタッチはボタンが消費する。これらは「録画対象外」であり、
 *   また再生中の一時停止トリガにもしない（仕様どおり）。
 *
 * 【重要な制約（公開APIの限界）】
 * 「再生用オーバーレイ以外の物理タッチ（配下アプリ上の任意のタッチ）を検知して
 *  一時停止する」ことは、公開APIでは非侵襲には実現できない。全画面のタッチ捕捉
 *  ビューを重ねると、dispatchGesture で注入したジェスチャー自身もそのビューへ
 *  吸われてしまい再生が成立しない。このため一時停止は本パネルの一時停止ボタンで
 *  行う実装とする。詳細は README「技術検証」を参照。
 */
class OverlayController(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var currentView: View? = null

    /** 録画用オーバーレイを表示する。 */
    fun showRecordOverlay(
        gravity: Int = Gravity.TOP or Gravity.END,
        onDiscard: () -> Unit,
        onSave: () -> Unit,
        onPause: () -> Unit,
        onResume: () -> Unit,
    ) {
        val panel = buildPanel(
            listOf(
                // 保存せず終了
                iconButton(android.R.drawable.ic_menu_close_clear_cancel, onDiscard),
                // 保存して終了
                iconButton(android.R.drawable.ic_menu_save, onSave),
                // 一時停止
                iconButton(android.R.drawable.ic_media_pause, onPause),
                // 再開
                iconButton(android.R.drawable.ic_media_play, onResume),
            ),
        )
        show(panel, gravity)
    }

    /**
     * 再生用オーバーレイを表示する。開始ボタンは一時停止ボタンへトグルする。
     * @return 開始/一時停止アイコンを切り替えるためのハンドル
     */
    fun showPlaybackOverlay(
        gravity: Int = Gravity.TOP or Gravity.END,
        onSettings: () -> Unit,
        onToggleStartPause: () -> Unit,
        onStop: () -> Unit,
    ): PlaybackOverlayHandle {
        // 設定
        val settingsBtn = iconButton(android.R.drawable.ic_menu_preferences, onSettings)
        // 開始/一時停止トグル
        val toggleBtn = iconButton(android.R.drawable.ic_media_play, onToggleStartPause)
        // 終了
        val stopBtn = iconButton(android.R.drawable.ic_menu_close_clear_cancel, onStop)

        val panel = buildPanel(listOf(settingsBtn, toggleBtn, stopBtn))
        show(panel, gravity)
        return PlaybackOverlayHandle(toggleBtn)
    }

    fun hide() {
        currentView?.let {
            runCatching { windowManager.removeView(it) }
        }
        currentView = null
    }

    // ---- 内部 ----

    private fun show(view: View, gravity: Int) {
        hide()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            // パネル外のタッチは横取りしない＝配下アプリを妨げない。
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.gravity = gravity
            x = 16
            y = 16
        }
        windowManager.addView(view, params)
        currentView = view
    }

    private fun buildPanel(buttons: List<View>): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.argb(160, 0, 0, 0))
            setPadding(8, 8, 8, 8)
            buttons.forEach { addView(it) }
        }

    private fun iconButton(iconRes: Int, onClick: () -> Unit): ImageButton =
        ImageButton(context).apply {
            setImageResource(iconRes)
            setBackgroundColor(Color.TRANSPARENT)
            val size = (48 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            setOnClickListener { onClick() }
        }

    /** 再生オーバーレイの開始/一時停止トグル用ハンドル。 */
    class PlaybackOverlayHandle(private val toggleButton: ImageButton) {
        fun showAsPlaying() = toggleButton.setImageResource(android.R.drawable.ic_media_pause)
        fun showAsPaused() = toggleButton.setImageResource(android.R.drawable.ic_media_play)
    }
}

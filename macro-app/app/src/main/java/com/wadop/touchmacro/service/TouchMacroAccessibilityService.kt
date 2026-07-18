package com.wadop.touchmacro.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.wadop.touchmacro.core.playback.PlaybackProgram
import com.wadop.touchmacro.core.playback.PlaybackState
import com.wadop.touchmacro.service.gesture.GestureDispatcher
import com.wadop.touchmacro.service.gesture.PlaybackController
import com.wadop.touchmacro.service.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * マクロの再生を担う AccessibilityService。
 *
 * 公開APIのうち以下を使用する:
 * - dispatchGesture: 記録した操作を注入して再生する（本アプリの中核・実現可能）。
 * - AccessibilityService のオーバーレイ権限併用で操作パネルを表示する。
 *
 * 画面内容の解析は行わない（canRetrieveWindowContent=false）。
 */
class TouchMacroAccessibilityService : AccessibilityService(), GestureDispatcher {

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlay: OverlayController? = null
    private var controller: PlaybackController? = null
    private var overlayHandle: OverlayController.PlaybackOverlayHandle? = null

    /** 画面消灯を検知して再生を安全に停止するレシーバ。 */
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                controller?.onScreenOff()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlay = OverlayController(this)
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        controller?.onServiceStopped()
        overlay?.hide()
        runCatching { unregisterReceiver(screenOffReceiver) }
        mainScope.cancel()
        instance = null
    }

    // AccessibilityService の必須オーバーライド（本アプリではイベント解析はしない）
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* 解析しない */ }
    override fun onInterrupt() { /* no-op */ }

    // ---- GestureDispatcher ----

    override fun dispatch(
        gesture: GestureDescription,
        onCompleted: () -> Unit,
        onCancelled: () -> Unit,
    ): Boolean {
        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onCompleted()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onCancelled()
            }
        }
        return dispatchGesture(gesture, callback, mainHandler)
    }

    // ---- 再生制御（アプリから呼ばれる） ----

    /**
     * 再生プログラムの再生を開始する。再生用オーバーレイを表示する。
     */
    fun startPlayback(program: PlaybackProgram, onFinished: (PlaybackState) -> Unit) {
        // 既存再生があれば停止
        controller?.stop()

        val ctrl = PlaybackController(
            program = program,
            scope = mainScope,
            dispatcher = this,
            onFinished = { finalState ->
                overlay?.hide()
                overlayHandle = null
                onFinished(finalState)
            },
        )
        controller = ctrl

        overlayHandle = overlay?.showPlaybackOverlay(
            onSettings = { /* 設定画面はアプリ側で開く。ここではイベント通知のみでも可 */ },
            onToggleStartPause = { toggleStartPause() },
            onStop = { ctrl.stop() },
        )
        ctrl.start()
        overlayHandle?.showAsPlaying()
    }

    private fun toggleStartPause() {
        val ctrl = controller ?: return
        when (ctrl.state.value) {
            is PlaybackState.PausedDuringGesture,
            is PlaybackState.PausedDuringWait,
            -> {
                ctrl.resume()
                overlayHandle?.showAsPlaying()
            }
            else -> {
                // 一時停止トリガ（オーバーレイの一時停止ボタン＝ユーザーの明示操作）。
                ctrl.pauseByPhysicalTouch()
                overlayHandle?.showAsPaused()
            }
        }
    }

    fun stopPlayback() {
        controller?.stop()
    }

    companion object {
        /** サービスの生存確認・再生要求のためのインスタンス参照。 */
        @Volatile
        var instance: TouchMacroAccessibilityService? = null
            private set

        /** ユーザー補助サービスが接続済みかどうか。 */
        val isConnected: Boolean get() = instance != null
    }
}

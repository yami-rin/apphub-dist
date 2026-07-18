package com.wadop.touchmacro.service.gesture

import android.os.SystemClock
import com.wadop.touchmacro.core.playback.PlaybackEffect
import com.wadop.touchmacro.core.playback.PlaybackProgram
import com.wadop.touchmacro.core.playback.PlaybackState
import com.wadop.touchmacro.core.playback.PlaybackStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 再生の実行ドライバ。純粋な状態機械([PlaybackStateMachine])に、
 * Android 側の副作用（dispatchGesture・待機タイマ）を接続する。
 *
 * 重要な設計:
 * - 状態遷移の判断は状態機械に一任し、ここは副作用の実行と結果通知だけを行う。
 * - dispatchGesture は送信済みのジェスチャーを途中キャンセルできないため、
 *   一時停止時は状態機械側を PausedDuringGesture にし、遅れて届く完了通知は
 *   ガードで無視させる（＝二重実行しない）。
 *
 * @param scope Main ディスパッチャ上のスコープを渡すこと
 * @param dispatcher ジェスチャー送信の実体（AccessibilityService）
 * @param onFinished 終了（Completed/Stopped/Error）時のコールバック
 */
class PlaybackController(
    val program: PlaybackProgram,
    private val scope: CoroutineScope,
    private val dispatcher: GestureDispatcher,
    private val onFinished: (PlaybackState) -> Unit,
) {
    private val machine = PlaybackStateMachine(program)

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    val completedLoops: Int get() = machine.completedLoops
    val lastError: String? get() = machine.lastError

    private var waitJob: Job? = null
    private var waitStartUptimeMs: Long = 0L
    private var currentWaitTotalMs: Long = 0L

    /** 再生開始。呼び出し前に権限・サービス接続を確認しておくこと。 */
    fun start() {
        machine.start()
        // 準備完了（権限確認は呼び出し側の責務）。
        machine.onPreparationComplete()
        enterCurrentState()
    }

    /** 物理タッチ検知による一時停止。 */
    fun pauseByPhysicalTouch() {
        // 待機中なら経過時間を計算して残りを保存させる。
        val elapsed = if (waitJob?.isActive == true) {
            SystemClock.uptimeMillis() - waitStartUptimeMs
        } else {
            0L
        }
        cancelWait()
        machine.onPhysicalUserTouch(elapsed)
        publish()
        // 一時停止状態では副作用を起こさない（enterCurrentState 不要）。
    }

    /** 再開。 */
    fun resume() {
        machine.resume()
        enterCurrentState()
    }

    /** 明示的な終了。 */
    fun stop() {
        cancelWait()
        machine.stop()
        enterCurrentState()
    }

    /** 画面消灯。 */
    fun onScreenOff() {
        cancelWait()
        machine.onScreenOff()
        enterCurrentState()
    }

    /** サービス停止。 */
    fun onServiceStopped() {
        cancelWait()
        machine.onServiceStopped()
        enterCurrentState()
    }

    /** 権限不足などの外的エラー。 */
    fun onError(reason: String) {
        cancelWait()
        machine.onError(reason)
        enterCurrentState()
    }

    // ---- 内部：状態に応じた副作用の実行 ----

    private fun enterCurrentState() {
        publish()
        when (val effect = machine.currentEffect()) {
            is PlaybackEffect.None -> Unit
            is PlaybackEffect.PrepareThenStart -> Unit // start() 内で処理済み
            is PlaybackEffect.DispatchGesture -> dispatchGesture(effect)
            is PlaybackEffect.StartWaitTimer -> startWaitTimer(effect.durationMs)
            is PlaybackEffect.Cleanup -> onFinished(machine.state)
        }
    }

    private fun dispatchGesture(effect: PlaybackEffect.DispatchGesture) {
        val gesture = GestureBuilder.build(effect.plannedOperation.operation)
        if (gesture == null) {
            // 送信不能な操作はキャンセル扱い（エラー記録して一時停止）。
            machine.onGestureCancelled("gesture_build_failed")
            enterCurrentState()
            return
        }
        val accepted = dispatcher.dispatch(
            gesture = gesture,
            onCompleted = {
                // ジェスチャー送信が完了＝操作完了（結果の正否は判定しない）。
                machine.onGestureCompleted()
                enterCurrentState()
            },
            onCancelled = {
                machine.onGestureCancelled("dispatch_cancelled")
                enterCurrentState()
            },
        )
        if (!accepted) {
            machine.onGestureCancelled("dispatch_rejected")
            enterCurrentState()
        }
    }

    private fun startWaitTimer(durationMs: Long) {
        cancelWait()
        if (durationMs <= 0L) {
            machine.onWaitElapsed()
            enterCurrentState()
            return
        }
        currentWaitTotalMs = durationMs
        waitStartUptimeMs = SystemClock.uptimeMillis()
        waitJob = scope.launch {
            delay(durationMs)
            if (isActive) {
                machine.onWaitElapsed()
                enterCurrentState()
            }
        }
    }

    private fun cancelWait() {
        waitJob?.cancel()
        waitJob = null
    }

    private fun publish() {
        _state.value = machine.state
    }
}

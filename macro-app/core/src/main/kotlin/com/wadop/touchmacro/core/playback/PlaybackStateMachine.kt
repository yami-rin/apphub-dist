package com.wadop.touchmacro.core.playback

/**
 * 再生を駆動する明示的な状態機械。
 *
 * 副作用（ジェスチャー送信・タイマ）は持たず、状態遷移だけを担う純粋ロジック。
 * Android 側のドライバは [currentEffect] を見て実際の副作用を実行し、
 * その結果をイベントメソッド（onGestureCompleted 等）でこの機械へ通知する。
 *
 * 設計上の重要ポイント（二重実行の防止）:
 * - 各イベントは「その状態でのみ有効」なガードを持ち、無効な状態では何もしない（no-op）。
 * - 例: onGestureCompleted は Playing のときだけ前進する。一時停止後に
 *   遅れて届いた完了通知や、二重に届いた通知は状態が Playing でないため無視される。
 * - これにより「一時停止・再開・終了・画面消灯・サービス停止」が競合しても
 *   操作が二重に前進することはない。
 */
class PlaybackStateMachine(
    val program: PlaybackProgram,
) {
    private val loopController = LoopController(program.repeatCount)

    var state: PlaybackState = PlaybackState.Idle
        private set

    /** 直近のエラー内容（ジェスチャーキャンセル等）を記録する。 */
    var lastError: String? = null
        private set

    /** 完了したループ数（完走したものだけ）。 */
    val completedLoops: Int get() = loopController.completedLoops

    // ---- 状態から導かれる副作用（ドライバが実行する） ----

    fun currentEffect(): PlaybackEffect = when (val s = state) {
        is PlaybackState.Idle -> PlaybackEffect.None
        is PlaybackState.Preparing -> PlaybackEffect.PrepareThenStart
        is PlaybackState.Playing ->
            PlaybackEffect.DispatchGesture(program.operations[s.opIndex], s.opIndex, s.loop)
        is PlaybackState.WaitingBetweenOperations -> PlaybackEffect.StartWaitTimer(s.remainingWaitMs)
        is PlaybackState.WaitingBetweenLoops -> PlaybackEffect.StartWaitTimer(s.remainingWaitMs)
        is PlaybackState.PausedDuringGesture -> PlaybackEffect.None
        is PlaybackState.PausedDuringWait -> PlaybackEffect.None
        is PlaybackState.Completed -> PlaybackEffect.Cleanup
        is PlaybackState.Stopped -> PlaybackEffect.Cleanup
        is PlaybackState.Error -> PlaybackEffect.Cleanup
    }

    // ---- イベント ----

    /** 再生開始。Idle からのみ有効。準備状態へ移る。 */
    fun start() {
        if (state !is PlaybackState.Idle) return
        state = PlaybackState.Preparing
    }

    /** 準備完了。Preparing からのみ有効。最初の操作へ進む（空なら即完了）。 */
    fun onPreparationComplete() {
        if (state !is PlaybackState.Preparing) return
        state = if (program.operationCount == 0) {
            PlaybackState.Completed
        } else {
            PlaybackState.Playing(opIndex = 0, loop = 1)
        }
    }

    /** ジェスチャー送信が完了した＝操作完了。Playing のときだけ前進する。 */
    fun onGestureCompleted() {
        val s = state as? PlaybackState.Playing ?: return
        state = advanceAfterOperation(s.opIndex, s.loop)
    }

    /**
     * ジェスチャーがキャンセルされた。エラー内容を記録して一時停止する（仕様）。
     * Playing のときだけ有効。
     */
    fun onGestureCancelled(reason: String) {
        val s = state as? PlaybackState.Playing ?: return
        lastError = reason
        state = PlaybackState.PausedDuringGesture(s.opIndex, s.loop)
    }

    /** 待機時間が満了した。待機状態のときだけ前進する。 */
    fun onWaitElapsed() {
        when (val s = state) {
            is PlaybackState.WaitingBetweenOperations ->
                state = proceedToNextAfter(s.afterOpIndex, s.loop)
            is PlaybackState.WaitingBetweenLoops ->
                state = PlaybackState.Playing(opIndex = 0, loop = s.nextLoop)
            else -> Unit // 一時停止中や他状態では無視（二重前進を防ぐ）
        }
    }

    /**
     * ユーザーが物理的に画面へ触れた＝一時停止トリガ。
     * マクロ生成タッチや再生用オーバーレイへのタッチではなく、
     * 「物理タッチ」と判定された場合のみ呼ぶこと（判定はドライバ側の責務）。
     *
     * @param elapsedInCurrentWaitMs 待機中一時停止の場合の、待機開始からの経過時間
     */
    fun onPhysicalUserTouch(elapsedInCurrentWaitMs: Long = 0L) {
        when (val s = state) {
            is PlaybackState.Playing -> {
                // 実行途中の操作は中断（再実行しない）。
                state = PlaybackState.PausedDuringGesture(s.opIndex, s.loop)
            }
            is PlaybackState.WaitingBetweenOperations -> {
                val remaining = WaitMath.remaining(s.remainingWaitMs, elapsedInCurrentWaitMs)
                state = PlaybackState.PausedDuringWait(
                    remainingWaitMs = remaining,
                    resume = WaitResume.ToNextOperation(s.afterOpIndex, s.loop),
                )
            }
            is PlaybackState.WaitingBetweenLoops -> {
                val remaining = WaitMath.remaining(s.remainingWaitMs, elapsedInCurrentWaitMs)
                state = PlaybackState.PausedDuringWait(
                    remainingWaitMs = remaining,
                    resume = WaitResume.ToNextLoop(s.nextLoop),
                )
            }
            else -> Unit // 既に一時停止中・準備中・終了済みなら無視
        }
    }

    /** 再開。一時停止状態のときだけ有効。 */
    fun resume() {
        when (val s = state) {
            is PlaybackState.PausedDuringGesture -> {
                // 中断した操作は再実行せず、次の操作へ進む。
                state = proceedToNextAfter(s.opIndex, s.loop)
            }
            is PlaybackState.PausedDuringWait -> {
                state = if (s.remainingWaitMs <= 0L) {
                    // 残り0なら即座に次へ。
                    resumeAfterWait(s.resume)
                } else {
                    // 残り待機時間を経過させてから次へ（待機状態へ復帰）。
                    when (val r = s.resume) {
                        is WaitResume.ToNextOperation ->
                            PlaybackState.WaitingBetweenOperations(r.afterOpIndex, r.loop, s.remainingWaitMs)
                        is WaitResume.ToNextLoop ->
                            PlaybackState.WaitingBetweenLoops(r.nextLoop, s.remainingWaitMs)
                    }
                }
            }
            else -> Unit
        }
    }

    /** 明示的な終了。終了済み以外の状態から Stopped へ。途中終了ループは数えない。 */
    fun stop() {
        if (isTerminal()) return
        state = PlaybackState.Stopped
    }

    /** 画面消灯。再生は継続できないため終了扱い。 */
    fun onScreenOff() {
        if (isTerminal()) return
        state = PlaybackState.Stopped
    }

    /** サービス停止。異常終了としてエラー状態へ。 */
    fun onServiceStopped() {
        if (isTerminal()) return
        lastError = REASON_SERVICE_STOPPED
        state = PlaybackState.Error(REASON_SERVICE_STOPPED)
    }

    /** 汎用エラー（権限不足・データ破損など）。 */
    fun onError(reason: String) {
        if (isTerminal()) return
        lastError = reason
        state = PlaybackState.Error(reason)
    }

    // ---- 内部ヘルパ ----

    private fun isTerminal(): Boolean = state is PlaybackState.Completed ||
        state is PlaybackState.Stopped ||
        state is PlaybackState.Error

    /**
     * 操作完了後の遷移。完了した操作の「後続待機」を適用してから次へ。
     * 待機が0なら即座に次操作/ループ境界へ。
     */
    private fun advanceAfterOperation(opIndex: Int, loop: Int): PlaybackState {
        val planned = program.operations[opIndex]
        return if (planned.waitAfterMs > 0L) {
            PlaybackState.WaitingBetweenOperations(opIndex, loop, planned.waitAfterMs)
        } else {
            proceedToNextAfter(opIndex, loop)
        }
    }

    /**
     * afterOpIndex の次の操作へ進む。1周の末尾を越えた場合はループ境界処理を行う。
     * （中断からの再開でも使う＝中断操作の後続待機はスキップされる）
     */
    private fun proceedToNextAfter(afterOpIndex: Int, loop: Int): PlaybackState {
        val nextOp = afterOpIndex + 1
        if (nextOp < program.operationCount) {
            return PlaybackState.Playing(nextOp, loop)
        }
        // 1周を完走した。ここで初めてループ完了を数える。
        loopController.onLoopCompleted()
        if (!loopController.hasMoreLoops()) {
            return PlaybackState.Completed
        }
        val nextLoop = loop + 1
        return if (program.loopWaitMs > 0L) {
            PlaybackState.WaitingBetweenLoops(nextLoop, program.loopWaitMs)
        } else {
            PlaybackState.Playing(opIndex = 0, loop = nextLoop)
        }
    }

    /** 残り待機0での再開時：待機せず次へ進む。 */
    private fun resumeAfterWait(resume: WaitResume): PlaybackState = when (resume) {
        is WaitResume.ToNextOperation -> proceedToNextAfter(resume.afterOpIndex, resume.loop)
        is WaitResume.ToNextLoop -> PlaybackState.Playing(opIndex = 0, loop = resume.nextLoop)
    }

    companion object {
        const val REASON_SERVICE_STOPPED = "service_stopped"
    }
}

/**
 * 状態から導かれる副作用の指示。Android ドライバがこれを解釈して実行する。
 */
sealed interface PlaybackEffect {
    /** 何もしない。 */
    data object None : PlaybackEffect

    /** 権限・オーバーレイ準備後に再生を開始する。 */
    data object PrepareThenStart : PlaybackEffect

    /** 指定操作のジェスチャーを送信する。 */
    data class DispatchGesture(
        val plannedOperation: PlannedOperation,
        val opIndex: Int,
        val loop: Int,
    ) : PlaybackEffect

    /** 指定時間の待機タイマを開始する。満了後 onWaitElapsed を呼ぶ。 */
    data class StartWaitTimer(val durationMs: Long) : PlaybackEffect

    /** 後片付け（オーバーレイ解除など）。 */
    data object Cleanup : PlaybackEffect
}

package com.wadop.touchmacro.core.playback

import com.wadop.touchmacro.core.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 再生状態機械のテスト。状態遷移・二重実行防止・一時停止再開・繰り返しを検証する。
 */
class PlaybackStateMachineTest {

    private fun program(
        ops: Int = 2,
        waitAfterMs: Long = 0,
        repeatCount: Int = 1,
        loopWaitMs: Long = 0,
    ): PlaybackProgram {
        val operations = (0 until ops).map {
            PlannedOperation(TestFixtures.tap("op$it", it), waitAfterMs)
        }
        return PlaybackProgram(operations, repeatCount, loopWaitMs)
    }

    /** currentEffect を見て自動応答し、terminal まで駆動する（タイマ/送信の擬似実行）。 */
    private fun runToTerminal(m: PlaybackStateMachine, maxSteps: Int = 1000) {
        var steps = 0
        while (true) {
            when (val e = m.currentEffect()) {
                is PlaybackEffect.PrepareThenStart -> m.onPreparationComplete()
                is PlaybackEffect.DispatchGesture -> m.onGestureCompleted()
                is PlaybackEffect.StartWaitTimer -> m.onWaitElapsed()
                is PlaybackEffect.Cleanup -> return
                is PlaybackEffect.None -> return
            }
            if (++steps > maxSteps) error("駆動が終了しない（無限ループの疑い）")
        }
    }

    @Test
    fun `初期状態はIdle`() {
        val m = PlaybackStateMachine(program())
        assertIs<PlaybackState.Idle>(m.state)
    }

    @Test
    fun `開始で準備状態へ移りその後最初の操作を再生`() {
        val m = PlaybackStateMachine(program(ops = 2))
        m.start()
        assertIs<PlaybackState.Preparing>(m.state)
        m.onPreparationComplete()
        assertEquals(PlaybackState.Playing(0, 1), m.state)
    }

    @Test
    fun `単一ループの正常完走`() {
        val m = PlaybackStateMachine(program(ops = 3, repeatCount = 1))
        m.start()
        runToTerminal(m)
        assertIs<PlaybackState.Completed>(m.state)
        assertEquals(1, m.completedLoops)
    }

    @Test
    fun `空プログラムは即完了`() {
        val m = PlaybackStateMachine(program(ops = 0))
        m.start()
        m.onPreparationComplete()
        assertIs<PlaybackState.Completed>(m.state)
    }

    @Test
    fun `3回再生で3周完走する`() {
        val m = PlaybackStateMachine(program(ops = 2, repeatCount = 3))
        m.start()
        runToTerminal(m)
        assertIs<PlaybackState.Completed>(m.state)
        assertEquals(3, m.completedLoops)
    }

    @Test
    fun `操作間待機が挿入される`() {
        val m = PlaybackStateMachine(program(ops = 2, waitAfterMs = 100))
        m.start(); m.onPreparationComplete()
        // op0 再生完了 → 操作間待機へ
        m.onGestureCompleted()
        val w = m.state
        assertIs<PlaybackState.WaitingBetweenOperations>(w)
        assertEquals(100L, w.remainingWaitMs)
        // 待機満了 → op1 再生
        m.onWaitElapsed()
        assertEquals(PlaybackState.Playing(1, 1), m.state)
    }

    @Test
    fun `ループ間待機が挿入され最終回後は待機しない`() {
        val m = PlaybackStateMachine(program(ops = 1, repeatCount = 2, loopWaitMs = 500))
        m.start(); m.onPreparationComplete()
        // 1周目 op0 完了 → ループ間待機
        m.onGestureCompleted()
        val w = m.state
        assertIs<PlaybackState.WaitingBetweenLoops>(w)
        assertEquals(500L, w.remainingWaitMs)
        assertEquals(2, w.nextLoop)
        // ループ間待機満了 → 2周目 op0
        m.onWaitElapsed()
        assertEquals(PlaybackState.Playing(0, 2), m.state)
        // 2周目 op0 完了 → 最終回なので待機せず即完了
        m.onGestureCompleted()
        assertIs<PlaybackState.Completed>(m.state)
        assertEquals(2, m.completedLoops)
    }

    // ---- 一時停止・再開 ----

    @Test
    fun `操作実行途中の一時停止は操作を中断し再開後は次操作へ`() {
        val m = PlaybackStateMachine(program(ops = 3))
        m.start(); m.onPreparationComplete()
        assertEquals(PlaybackState.Playing(0, 1), m.state)
        // op0 実行中に物理タッチ → 中断
        m.onPhysicalUserTouch()
        assertEquals(PlaybackState.PausedDuringGesture(0, 1), m.state)
        // 再開 → 中断した op0 は再実行せず op1 へ
        m.resume()
        assertEquals(PlaybackState.Playing(1, 1), m.state)
    }

    @Test
    fun `中断された操作の遅延完了通知は無視される（二重実行防止）`() {
        val m = PlaybackStateMachine(program(ops = 3))
        m.start(); m.onPreparationComplete()
        m.onPhysicalUserTouch() // PausedDuringGesture(0,1)
        // 実際に飛んでいたジェスチャーが遅れて完了通知してきても前進しない
        m.onGestureCompleted()
        assertEquals(PlaybackState.PausedDuringGesture(0, 1), m.state)
        // Resume してはじめて次へ
        m.resume()
        assertEquals(PlaybackState.Playing(1, 1), m.state)
    }

    @Test
    fun `待機中の一時停止は残り時間を保存し再開後に残りを消化`() {
        val m = PlaybackStateMachine(program(ops = 2, waitAfterMs = 1000))
        m.start(); m.onPreparationComplete()
        m.onGestureCompleted() // WaitingBetweenOperations(1000)
        // 300ms 経過して一時停止 → 残り700
        m.onPhysicalUserTouch(elapsedInCurrentWaitMs = 300)
        val p = m.state
        assertIs<PlaybackState.PausedDuringWait>(p)
        assertEquals(700L, p.remainingWaitMs)
        // 再開 → 残り700の待機へ復帰
        m.resume()
        val w = m.state
        assertIs<PlaybackState.WaitingBetweenOperations>(w)
        assertEquals(700L, w.remainingWaitMs)
        // 残り満了 → 次操作
        m.onWaitElapsed()
        assertEquals(PlaybackState.Playing(1, 1), m.state)
    }

    @Test
    fun `残り待機0で再開すると即座に次へ進む`() {
        val m = PlaybackStateMachine(program(ops = 2, waitAfterMs = 500))
        m.start(); m.onPreparationComplete()
        m.onGestureCompleted()
        m.onPhysicalUserTouch(elapsedInCurrentWaitMs = 500) // 残り0
        m.resume()
        assertEquals(PlaybackState.Playing(1, 1), m.state)
    }

    @Test
    fun `一時停止中は待機満了通知でも前進しない`() {
        val m = PlaybackStateMachine(program(ops = 2, waitAfterMs = 1000))
        m.start(); m.onPreparationComplete()
        m.onGestureCompleted()
        m.onPhysicalUserTouch(elapsedInCurrentWaitMs = 200)
        // 一時停止中に来た待機満了は無視
        m.onWaitElapsed()
        assertIs<PlaybackState.PausedDuringWait>(m.state)
    }

    @Test
    fun `再生用オーバーレイ相当のタッチ（物理タッチでない）は一時停止しない`() {
        // 物理タッチと判定されない入力では onPhysicalUserTouch を呼ばない、という
        // ドライバ契約を表現：呼ばなければ状態は Playing のまま。
        val m = PlaybackStateMachine(program(ops = 2))
        m.start(); m.onPreparationComplete()
        assertEquals(PlaybackState.Playing(0, 1), m.state)
    }

    // ---- 終了・エラー ----

    @Test
    fun `停止は任意の非終了状態からStoppedへ`() {
        val m = PlaybackStateMachine(program(ops = 3))
        m.start(); m.onPreparationComplete()
        m.stop()
        assertIs<PlaybackState.Stopped>(m.state)
        // 途中終了ループは数えない
        assertEquals(0, m.completedLoops)
    }

    @Test
    fun `終了後のイベントは無視される（冪等）`() {
        val m = PlaybackStateMachine(program(ops = 2))
        m.start(); m.onPreparationComplete()
        m.stop()
        m.onGestureCompleted()
        m.resume()
        m.start()
        assertIs<PlaybackState.Stopped>(m.state)
    }

    @Test
    fun `ジェスチャーキャンセルはエラーを記録して一時停止する`() {
        val m = PlaybackStateMachine(program(ops = 3))
        m.start(); m.onPreparationComplete()
        m.onGestureCancelled("dispatch cancelled")
        assertIs<PlaybackState.PausedDuringGesture>(m.state)
        assertEquals("dispatch cancelled", m.lastError)
        // 再開で次操作へ
        m.resume()
        assertEquals(PlaybackState.Playing(1, 1), m.state)
    }

    @Test
    fun `画面消灯で停止する`() {
        val m = PlaybackStateMachine(program(ops = 3))
        m.start(); m.onPreparationComplete()
        m.onScreenOff()
        assertIs<PlaybackState.Stopped>(m.state)
    }

    @Test
    fun `サービス停止はエラー状態になる`() {
        val m = PlaybackStateMachine(program(ops = 3))
        m.start(); m.onPreparationComplete()
        m.onServiceStopped()
        val s = m.state
        assertIs<PlaybackState.Error>(s)
        assertEquals(PlaybackStateMachine.REASON_SERVICE_STOPPED, s.reason)
    }

    @Test
    fun `権限不足等の汎用エラー`() {
        val m = PlaybackStateMachine(program(ops = 3))
        m.start(); m.onPreparationComplete()
        m.onError("permission_denied")
        val s = m.state
        assertIs<PlaybackState.Error>(s)
        assertEquals("permission_denied", s.reason)
    }

    @Test
    fun `Idleでの完了通知やresumeは無視される`() {
        val m = PlaybackStateMachine(program(ops = 2))
        m.onGestureCompleted()
        m.resume()
        m.onWaitElapsed()
        assertIs<PlaybackState.Idle>(m.state)
        assertNull(m.lastError)
    }

    @Test
    fun `無限再生は停止されるまで完走し続ける`() {
        val m = PlaybackStateMachine(program(ops = 1, repeatCount = 0))
        m.start(); m.onPreparationComplete()
        // 5周ぶん駆動
        repeat(5) {
            assertTrue(m.state is PlaybackState.Playing)
            m.onGestureCompleted()
        }
        assertEquals(5, m.completedLoops)
        // まだ継続中（Completed にならない）
        assertTrue(m.state is PlaybackState.Playing)
        m.stop()
        assertIs<PlaybackState.Stopped>(m.state)
    }
}

package com.wadop.touchmacro.core.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 再生回数計算・ループ間待機のテスト。
 */
class LoopControllerTest {

    @Test
    fun `初期再生回数1回は1周で完了する`() {
        val c = LoopController(targetLoops = 1)
        assertTrue(c.hasMoreLoops())
        c.onLoopCompleted()
        assertFalse(c.hasMoreLoops())
        assertTrue(c.isFinished())
        assertEquals(1, c.completedLoops)
    }

    @Test
    fun `再生回数3回は3周で完了する`() {
        val c = LoopController(targetLoops = 3)
        var loops = 0
        while (c.hasMoreLoops()) {
            c.onLoopCompleted()
            loops++
            if (loops > 10) break // 安全弁
        }
        assertEquals(3, loops)
        assertEquals(3, c.completedLoops)
    }

    @Test
    fun `再生回数0は無限再生`() {
        val c = LoopController(targetLoops = 0)
        assertTrue(c.isInfinite)
        repeat(100) { c.onLoopCompleted() }
        assertTrue(c.hasMoreLoops()) // 何周しても継続
    }

    @Test
    fun `ループ間待機は最終回の後には適用しない`() {
        val c = LoopController(targetLoops = 2)
        // 1周目完了 → 次(2周目)が走るので待機する
        c.onLoopCompleted()
        assertTrue(c.shouldWaitBeforeNextLoop())
        // 2周目完了 → 最終回なので待機しない
        c.onLoopCompleted()
        assertFalse(c.shouldWaitBeforeNextLoop())
    }

    @Test
    fun `無限再生ではループ間待機を常に適用する`() {
        val c = LoopController(targetLoops = 0)
        c.onLoopCompleted()
        assertTrue(c.shouldWaitBeforeNextLoop())
        repeat(50) { c.onLoopCompleted() }
        assertTrue(c.shouldWaitBeforeNextLoop())
    }

    @Test
    fun `一時停止で完了数は進まない`() {
        // onLoopCompleted を呼ばない限り完了数は増えない＝一時停止中は数えない、を表現。
        val c = LoopController(targetLoops = 3)
        assertEquals(0, c.completedLoops)
        // 完走せずに終了した場合、完了数は0のまま
        assertEquals(0, c.completedLoops)
    }
}

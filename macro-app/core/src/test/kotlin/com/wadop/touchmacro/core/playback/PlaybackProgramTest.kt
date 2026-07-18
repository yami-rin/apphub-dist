package com.wadop.touchmacro.core.playback

import com.wadop.touchmacro.core.TestFixtures
import com.wadop.touchmacro.core.unit.UnitBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 録画マクロ／ユニット → 再生プログラム平坦化のテスト。
 */
class PlaybackProgramTest {

    @Test
    fun `録画マクロは操作順に平坦化され待機が引き継がれる`() {
        val rec = TestFixtures.recording(
            operations = listOf(
                TestFixtures.tap("a", 0, waitAfterMs = 100),
                TestFixtures.tap("b", 1, waitAfterMs = 0),
            ),
        )
        val prog = PlaybackProgram.fromRecording(rec, repeatCount = 2, loopWaitMs = 500)
        assertEquals(2, prog.operationCount)
        assertEquals(100L, prog.operations[0].waitAfterMs)
        assertEquals(2, prog.repeatCount)
        assertEquals(500L, prog.loopWaitMs)
    }

    @Test
    fun `ユニットは要素末尾の追加待機を最後の操作へ加算する`() {
        val rec = TestFixtures.recording(
            operations = listOf(
                TestFixtures.tap("a", 0, waitAfterMs = 100),
                TestFixtures.tap("b", 1, waitAfterMs = 50),
            ),
        )
        val builder = UnitBuilder(TestFixtures.sequentialIdGenerator())
        var unit = builder.createUnit("u1", "unit", 0, listOf(rec))
        unit = builder.changeExtraWait(unit, 0, 300)

        val prog = PlaybackProgram.fromUnit(unit)
        assertEquals(2, prog.operationCount)
        // 最初の操作はそのまま
        assertEquals(100L, prog.operations[0].waitAfterMs)
        // 最後の操作に追加待機300が加算される: 50+300=350
        assertEquals(350L, prog.operations[1].waitAfterMs)
    }

    @Test
    fun `複数要素のユニットは連結される`() {
        val recA = TestFixtures.recording(id = "A", operations = listOf(TestFixtures.tap("a", 0)))
        val recB = TestFixtures.recording(
            id = "B",
            operations = listOf(TestFixtures.tap("b", 0), TestFixtures.tap("c", 1)),
        )
        val builder = UnitBuilder(TestFixtures.sequentialIdGenerator())
        val unit = builder.createUnit("u1", "unit", 0, listOf(recA, recB), repeatCount = 3, loopWaitMs = 1000)
        val prog = PlaybackProgram.fromUnit(unit)
        assertEquals(3, prog.operationCount)
        assertEquals(3, prog.repeatCount)
        assertEquals(1000L, prog.loopWaitMs)
    }
}

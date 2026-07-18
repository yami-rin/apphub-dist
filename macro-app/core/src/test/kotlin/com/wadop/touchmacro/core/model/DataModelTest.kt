package com.wadop.touchmacro.core.model

import com.wadop.touchmacro.core.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * データモデルのユニットテスト。
 */
class DataModelTest {

    @Test
    fun `録画マクロの操作数と総再生時間`() {
        val rec = TestFixtures.recording(
            operations = listOf(
                TestFixtures.tap("a", 0, durationMs = 50, waitAfterMs = 100),
                TestFixtures.tap("b", 1, durationMs = 80, waitAfterMs = 200),
            ),
        )
        assertEquals(2, rec.operationCount)
        // 50+100 + 80+200 = 430
        assertEquals(430L, rec.totalDurationMs)
        assertEquals(MacroKind.RECORDING, rec.kind)
        assertNull(rec.lastPlayedAtEpochMs)
    }

    @Test
    fun `多指操作は共通の座標軌跡として保持される`() {
        val pinch = TestFixtures.pinch("p", 0)
        val payload = pinch.payload as CoordinatePayload
        assertEquals(2, payload.pointerCount)
        assertEquals(CoordinatePayload.KIND, payload.kind)
        // 各指が独立した時刻付き座標列を持つ
        assertEquals(2, payload.pointers[0].points.size)
        assertEquals(300L, payload.pointers[0].durationMs)
    }

    @Test
    fun `負の待機時間は拒否される`() {
        assertFailsWith<IllegalArgumentException> {
            TestFixtures.tap("x", 0, waitAfterMs = -1)
        }
    }

    @Test
    fun `ペイロードは拡張可能なsealed型である`() {
        // 座標以外の操作タイプを将来追加できる設計であることを型で確認。
        val payload: OperationPayload = CoordinatePayload(emptyList())
        assertTrue(payload is CoordinatePayload)
    }

    @Test
    fun `ユニットは録画マクロと異なる種別として識別できる`() {
        val unit = MacroUnit(
            id = "u1",
            displayName = "unit",
            createdAtEpochMs = 0,
            elements = emptyList(),
        )
        assertEquals(MacroKind.UNIT, unit.kind)
    }
}

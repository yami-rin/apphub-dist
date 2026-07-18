package com.wadop.touchmacro.core.unit

import com.wadop.touchmacro.core.TestFixtures
import com.wadop.touchmacro.core.model.CoordinatePayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * ユニット（コピー方式）のテスト。
 */
class UnitBuilderTest {

    private fun builder() = UnitBuilder(TestFixtures.sequentialIdGenerator())

    @Test
    fun `ユニット作成時に操作データが完全複製される`() {
        val rec = TestFixtures.recording(
            operations = listOf(
                TestFixtures.tap("orig-a", 0, x = 10f, y = 20f),
                TestFixtures.tap("orig-b", 1),
            ),
        )
        val unit = builder().createUnit("u1", "unit", 0, listOf(rec))

        val element = unit.elements.single()
        assertEquals(2, element.operations.size)
        // 操作IDは元と独立（振り直し）されている
        assertNotEquals("orig-a", element.operations[0].id)
        // 座標データは同値でコピーされている
        val copied = element.operations[0].payload as CoordinatePayload
        assertEquals(10f, copied.pointers[0].points[0].x)
        // 由来情報は保持
        assertEquals(rec.id, element.sourceRecordingId)
    }

    @Test
    fun `元マクロを削除または編集してもユニットは影響を受けない`() {
        val rec = TestFixtures.recording(
            id = "rec-src",
            operations = listOf(TestFixtures.tap("a", 0, x = 10f)),
        )
        val builder = builder()
        val unit = builder.createUnit("u1", "unit", 0, listOf(rec))

        // 元マクロ側で「削除」= 参照を捨てる／「編集」= 別インスタンス化 をシミュレート。
        // ユニットは値コピーを保持しているため、元が消えても操作データは残る。
        val snapshotX = (unit.elements[0].operations[0].payload as CoordinatePayload)
            .pointers[0].points[0].x
        assertEquals(10f, snapshotX)

        // 元マクロを「編集」しても（新インスタンス）、既存ユニットのデータは不変。
        @Suppress("UNUSED_VARIABLE")
        val editedRec = rec.copy(operations = listOf(TestFixtures.tap("a", 0, x = 999f)))
        val stillX = (unit.elements[0].operations[0].payload as CoordinatePayload)
            .pointers[0].points[0].x
        assertEquals(10f, stillX)
    }

    @Test
    fun `同じ録画マクロを複数回追加できる`() {
        val rec = TestFixtures.recording(operations = listOf(TestFixtures.tap("a", 0)))
        val unit = builder().createUnit("u1", "unit", 0, listOf(rec, rec, rec))
        assertEquals(3, unit.elements.size)
        // 各要素は別 elementId を持つ
        assertEquals(3, unit.elements.map { it.elementId }.toSet().size)
        // 各要素の操作IDも重複しない
        val allOpIds = unit.elements.flatMap { it.operations.map { op -> op.id } }
        assertEquals(allOpIds.size, allOpIds.toSet().size)
    }

    @Test
    fun `構成要素の順番入れ替え`() {
        val recA = TestFixtures.recording(id = "A", operations = listOf(TestFixtures.tap("a", 0)))
        val recB = TestFixtures.recording(id = "B", operations = listOf(TestFixtures.tap("b", 0)))
        val builder = builder()
        val unit = builder.createUnit("u1", "unit", 0, listOf(recA, recB))
        val moved = builder.moveElement(unit, from = 0, to = 1)
        assertEquals("B", moved.elements[0].sourceRecordingId)
        assertEquals("A", moved.elements[1].sourceRecordingId)
    }

    @Test
    fun `構成要素の複製`() {
        val rec = TestFixtures.recording(operations = listOf(TestFixtures.tap("a", 0)))
        val builder = builder()
        val unit = builder.createUnit("u1", "unit", 0, listOf(rec))
        val dup = builder.duplicateElement(unit, 0)
        assertEquals(2, dup.elements.size)
        // 複製要素は別 elementId・別 操作ID
        assertNotEquals(dup.elements[0].elementId, dup.elements[1].elementId)
        assertNotEquals(dup.elements[0].operations[0].id, dup.elements[1].operations[0].id)
    }

    @Test
    fun `構成要素の削除`() {
        val recA = TestFixtures.recording(id = "A", operations = listOf(TestFixtures.tap("a", 0)))
        val recB = TestFixtures.recording(id = "B", operations = listOf(TestFixtures.tap("b", 0)))
        val builder = builder()
        val unit = builder.createUnit("u1", "unit", 0, listOf(recA, recB))
        val removed = builder.removeElement(unit, 0)
        assertEquals(1, removed.elements.size)
        assertEquals("B", removed.elements[0].sourceRecordingId)
    }

    @Test
    fun `構成要素終了後の追加待機を個別変更できる`() {
        val rec = TestFixtures.recording(operations = listOf(TestFixtures.tap("a", 0)))
        val builder = builder()
        val unit = builder.createUnit("u1", "unit", 0, listOf(rec))
        val changed = builder.changeExtraWait(unit, 0, 500)
        assertEquals(500L, changed.elements[0].extraWaitAfterMs)
    }

    @Test
    fun `ユニットの操作数と総再生時間は全要素の合計`() {
        val rec = TestFixtures.recording(
            operations = listOf(
                TestFixtures.tap("a", 0, durationMs = 50, waitAfterMs = 100),
            ),
        )
        val builder = builder()
        var unit = builder.createUnit("u1", "unit", 0, listOf(rec, rec))
        unit = builder.changeExtraWait(unit, 0, 200)
        // 要素0: 50+100+200=350, 要素1: 50+100=150, 合計 500
        assertEquals(500L, unit.totalDurationMs)
        assertEquals(2, unit.operationCount)
    }

    @Test
    fun `deep copyにより元とユニットの座標リストは参照共有しない`() {
        val rec = TestFixtures.recording(operations = listOf(TestFixtures.pinch("p", 0)))
        val unit = builder().createUnit("u1", "unit", 0, listOf(rec))
        val origPoints = (rec.operations[0].payload as CoordinatePayload).pointers[0].points
        val copyPoints = (unit.elements[0].operations[0].payload as CoordinatePayload).pointers[0].points
        // 値は等しいが同一インスタンスではない
        assertEquals(origPoints, copyPoints)
        assertTrue(origPoints !== copyPoints)
    }
}

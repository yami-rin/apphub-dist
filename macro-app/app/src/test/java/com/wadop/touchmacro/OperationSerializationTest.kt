package com.wadop.touchmacro

import com.wadop.touchmacro.core.model.CoordinatePayload
import com.wadop.touchmacro.core.model.Operation
import com.wadop.touchmacro.core.model.OperationStatus
import com.wadop.touchmacro.core.model.PointerTrace
import com.wadop.touchmacro.core.model.TracePoint
import com.wadop.touchmacro.data.serialization.OperationSerialization
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 操作データの JSON 直列化・復元の往復テスト（JVM 上で実行可能）。
 * データ破損に強くするため、保存→読込で完全一致することを確認する。
 */
class OperationSerializationTest {

    private fun sampleOps(): List<Operation> = listOf(
        Operation(
            id = "op1",
            index = 0,
            startedAtEpochMs = 1000,
            durationMs = 120,
            elapsedFromStartMs = 0,
            status = OperationStatus.COMPLETED,
            waitAfterMs = 300,
            payload = CoordinatePayload(
                pointers = listOf(
                    PointerTrace(0, listOf(TracePoint(0, 10f, 20f), TracePoint(120, 30f, 40f))),
                    PointerTrace(1, listOf(TracePoint(0, 50f, 60f, 0.5f))),
                ),
            ),
        ),
    )

    @Test
    fun `操作列の直列化と復元が一致する`() {
        val ops = sampleOps()
        val json = OperationSerialization.encodeOperations(ops)
        val restored = OperationSerialization.decodeOperations(json)
        assertEquals(ops, restored)
    }

    @Test
    fun `多指座標のペイロード種別が保持される`() {
        val ops = sampleOps()
        val json = OperationSerialization.encodeOperations(ops)
        val restored = OperationSerialization.decodeOperations(json)
        val payload = restored[0].payload as CoordinatePayload
        assertEquals(2, payload.pointerCount)
        assertEquals(0.5f, payload.pointers[1].points[0].pressure)
    }
}

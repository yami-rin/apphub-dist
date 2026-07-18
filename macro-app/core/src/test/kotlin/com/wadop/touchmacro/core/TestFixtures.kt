package com.wadop.touchmacro.core

import com.wadop.touchmacro.core.model.CoordinatePayload
import com.wadop.touchmacro.core.model.Operation
import com.wadop.touchmacro.core.model.OperationStatus
import com.wadop.touchmacro.core.model.PointerTrace
import com.wadop.touchmacro.core.model.Recording
import com.wadop.touchmacro.core.model.TracePoint

/**
 * テスト用のデータ生成ヘルパ。
 */
object TestFixtures {

    /** 単純なタップ操作を1つ作る。 */
    fun tap(
        id: String,
        index: Int,
        x: Float = 100f,
        y: Float = 100f,
        durationMs: Long = 50,
        waitAfterMs: Long = 0,
        elapsedFromStartMs: Long = 0,
    ): Operation = Operation(
        id = id,
        index = index,
        startedAtEpochMs = 1_000L + elapsedFromStartMs,
        durationMs = durationMs,
        elapsedFromStartMs = elapsedFromStartMs,
        status = OperationStatus.COMPLETED,
        waitAfterMs = waitAfterMs,
        payload = CoordinatePayload(
            pointers = listOf(
                PointerTrace(
                    pointerId = 0,
                    points = listOf(
                        TracePoint(0, x, y),
                        TracePoint(durationMs, x, y),
                    ),
                ),
            ),
        ),
    )

    /** 2本指ピンチ相当の操作を作る（多指軌跡の共通表現確認用）。 */
    fun pinch(id: String, index: Int, waitAfterMs: Long = 0): Operation = Operation(
        id = id,
        index = index,
        startedAtEpochMs = 2_000L,
        durationMs = 300,
        elapsedFromStartMs = 0,
        status = OperationStatus.COMPLETED,
        waitAfterMs = waitAfterMs,
        payload = CoordinatePayload(
            pointers = listOf(
                PointerTrace(0, listOf(TracePoint(0, 200f, 200f), TracePoint(300, 100f, 100f))),
                PointerTrace(1, listOf(TracePoint(0, 400f, 400f), TracePoint(300, 500f, 500f))),
            ),
        ),
    )

    /** operations から録画マクロを作る。 */
    fun recording(
        id: String = "rec-1",
        displayName: String = "20260718-1",
        operations: List<Operation>,
        createdAtEpochMs: Long = 10_000L,
    ): Recording = Recording(
        id = id,
        displayName = displayName,
        createdAtEpochMs = createdAtEpochMs,
        operations = operations,
    )

    /** 連番IDジェネレータ（決定的）。 */
    fun sequentialIdGenerator(prefix: String = "gen-"): () -> String {
        var n = 0
        return { "$prefix${n++}" }
    }
}

package com.wadop.touchmacro.service.gesture

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.wadop.touchmacro.core.model.CoordinatePayload
import com.wadop.touchmacro.core.model.Operation
import com.wadop.touchmacro.core.model.PointerTrace

/**
 * core の操作([Operation]) を Android の [GestureDescription] に変換する。
 *
 * 各指([PointerTrace]) を1本のストロークにし、複数指はストロークを並べることで
 * 多指ジェスチャー（ピンチ等）を表現する。dispatchGesture は最大10ストローク、
 * 総時間に上限があるため、それを超える場合は分割/クランプが必要（本実装ではクランプ）。
 *
 * 対応API: StrokeDescription は API 24+。複数ストローク同時実行も API 24+ で可能。
 */
object GestureBuilder {

    /** GestureDescription の1ジェスチャー最大時間。端末実装により前後する保守値。 */
    const val MAX_GESTURE_DURATION_MS = 60_000L

    /** 単一点（タップ）を線分として表現するための最小継続時間。 */
    private const val MIN_STROKE_DURATION_MS = 1L

    /**
     * 操作からジェスチャーを生成する。座標ペイロード以外は現状 null（未対応）を返す。
     */
    fun build(operation: Operation): GestureDescription? {
        val payload = operation.payload as? CoordinatePayload ?: return null
        if (payload.pointers.isEmpty()) return null

        val builder = GestureDescription.Builder()
        var strokeCount = 0
        for (trace in payload.pointers) {
            if (strokeCount >= GestureDescription.getMaxStrokeCount()) break
            val stroke = buildStroke(trace) ?: continue
            builder.addStroke(stroke)
            strokeCount++
        }
        if (strokeCount == 0) return null
        return builder.build()
    }

    /** 1本の指の軌跡からストロークを作る。 */
    private fun buildStroke(trace: PointerTrace): GestureDescription.StrokeDescription? {
        val points = trace.points
        if (points.isEmpty()) return null

        val path = Path()
        val first = points.first()
        path.moveTo(first.x, first.y)
        if (points.size == 1) {
            // タップ：同一座標へ線を引き、極短時間のストロークにする。
            path.lineTo(first.x, first.y)
        } else {
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
        }

        val startTime = points.first().tOffsetMs.coerceAtLeast(0)
        val rawDuration = (points.last().tOffsetMs - points.first().tOffsetMs)
            .coerceAtLeast(MIN_STROKE_DURATION_MS)
        // 総時間の上限にクランプ（開始+継続が上限を超えないように）。
        val duration = rawDuration.coerceAtMost(MAX_GESTURE_DURATION_MS - startTime)
            .coerceAtLeast(MIN_STROKE_DURATION_MS)

        return GestureDescription.StrokeDescription(path, startTime, duration)
    }
}

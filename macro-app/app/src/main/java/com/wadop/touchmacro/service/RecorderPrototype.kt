package com.wadop.touchmacro.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.wadop.touchmacro.core.model.CoordinatePayload
import com.wadop.touchmacro.core.model.Operation
import com.wadop.touchmacro.core.model.OperationStatus
import com.wadop.touchmacro.core.model.PointerTrace
import com.wadop.touchmacro.core.model.TracePoint
import java.util.UUID

/**
 * 【技術検証プロトタイプ】物理タッチ座標の取得。
 *
 * このクラスは「全画面の透過タッチ捕捉ビュー」を重ね、その上で発生した
 * MotionEvent から多指の時刻付き座標列を取得できることを実証する。
 * タップ/長押し/スワイプ/ドラッグ/多指/ピンチはいずれも MotionEvent の
 * ポインタ列として共通に取得でき、[Operation]（共通の複数指軌跡データ）へ変換できる。
 *
 * 【原理的な制約（重要・誤魔化さない）】
 * この捕捉ビューはタッチを「消費」するため、配下アプリにはタッチが届かない。
 * すなわち本方式では「対象アプリの通常操作を妨げずに記録」は満たせない。
 * 非root・公開APIのみでは、
 *   - 透過(FLAG_NOT_TOUCHABLE)にすると座標を取得できない
 *   - 取得可能にすると配下アプリを妨げる
 * のいずれかにしかならず、両立は不可能。詳細と代替案は README「技術検証」を参照。
 *
 * よって本クラスは「取得できること」の検証用であり、
 * 実運用の無干渉録画としては使用できない。
 */
class RecorderPrototype(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var captureView: TouchCaptureView? = null

    /** 記録された操作列（1タッチ相互作用＝1操作）。 */
    val recordedOperations = mutableListOf<Operation>()

    private var recordingStartUptimeMs: Long = 0L
    private var operationIndex = 0

    /** 捕捉を開始する（全画面の捕捉ビューを重ねる）。 */
    fun start() {
        if (captureView != null) return
        recordingStartUptimeMs = android.os.SystemClock.uptimeMillis()
        operationIndex = 0
        recordedOperations.clear()

        val view = TouchCaptureView(context) { op -> recordedOperations.add(op) }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            // タッチを受け取るため NOT_TOUCHABLE は付けない＝配下アプリを妨げる（制約）。
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        )
        windowManager.addView(view, params)
        captureView = view
    }

    /** 捕捉を終了する。 */
    fun stop() {
        captureView?.let { runCatching { windowManager.removeView(it) } }
        captureView = null
    }

    /**
     * 全画面のタッチ捕捉ビュー。MotionEvent を多指の座標列として蓄積し、
     * 1相互作用（最初の接地〜全指解放）を1操作として確定する。
     */
    @SuppressLint("ViewConstructor")
    private inner class TouchCaptureView(
        context: Context,
        private val onOperationComplete: (Operation) -> Unit,
    ) : View(context) {

        // pointerId -> 座標列
        private val traces = LinkedHashMap<Int, MutableList<TracePoint>>()
        private var opStartEpochMs = 0L
        private var opStartUptimeMs = 0L

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // 最初の指が触れた＝操作開始
                    traces.clear()
                    opStartEpochMs = System.currentTimeMillis()
                    opStartUptimeMs = event.eventTime
                    appendCurrent(event)
                }
                MotionEvent.ACTION_POINTER_DOWN -> appendCurrent(event)
                MotionEvent.ACTION_MOVE -> appendHistoryAndCurrent(event)
                MotionEvent.ACTION_POINTER_UP -> appendCurrent(event)
                MotionEvent.ACTION_UP -> {
                    appendCurrent(event)
                    finalizeOperation(cancelled = false)
                }
                MotionEvent.ACTION_CANCEL -> {
                    finalizeOperation(cancelled = true)
                }
            }
            return true // 消費する（＝配下アプリには届かない：制約）
        }

        /** 現在フレームの各ポインタ座標を追加。 */
        private fun appendCurrent(event: MotionEvent) {
            for (i in 0 until event.pointerCount) {
                val id = event.getPointerId(i)
                val t = event.eventTime - opStartUptimeMs
                traces.getOrPut(id) { mutableListOf() }
                    .add(TracePoint(t, event.getX(i), event.getY(i), event.getPressure(i)))
            }
        }

        /** MOVE の履歴サンプルも取りこぼさず追加（滑らかな軌跡のため）。 */
        private fun appendHistoryAndCurrent(event: MotionEvent) {
            val historySize = event.historySize
            for (h in 0 until historySize) {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val t = event.getHistoricalEventTime(h) - opStartUptimeMs
                    traces.getOrPut(id) { mutableListOf() }.add(
                        TracePoint(
                            t,
                            event.getHistoricalX(i, h),
                            event.getHistoricalY(i, h),
                            event.getHistoricalPressure(i, h),
                        ),
                    )
                }
            }
            appendCurrent(event)
        }

        private fun finalizeOperation(cancelled: Boolean) {
            if (traces.isEmpty()) return
            val pointers = traces.map { (id, points) -> PointerTrace(id, points.toList()) }
            val duration = pointers.maxOfOrNull { it.points.lastOrNull()?.tOffsetMs ?: 0L } ?: 0L
            val op = Operation(
                id = UUID.randomUUID().toString(),
                index = operationIndex++,
                startedAtEpochMs = opStartEpochMs,
                durationMs = duration,
                elapsedFromStartMs = opStartUptimeMs - recordingStartUptimeMs,
                status = if (cancelled) OperationStatus.CANCELLED else OperationStatus.COMPLETED,
                // 次操作までの待機は、次の ACTION_DOWN までの間隔で後段が補正する想定。
                waitAfterMs = 0L,
                payload = CoordinatePayload(pointers),
            )
            onOperationComplete(op)
            traces.clear()
        }
    }
}

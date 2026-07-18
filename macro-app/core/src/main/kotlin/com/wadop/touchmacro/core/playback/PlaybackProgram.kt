package com.wadop.touchmacro.core.playback

import com.wadop.touchmacro.core.model.MacroUnit
import com.wadop.touchmacro.core.model.Operation
import com.wadop.touchmacro.core.model.Recording

/**
 * 再生1操作分。操作本体と、その操作後の待機時間をまとめる。
 * ユニットの場合、要素末尾の追加待機は最後の操作の [waitAfterMs] へ加算済み。
 */
data class PlannedOperation(
    val operation: Operation,
    val waitAfterMs: Long,
)

/**
 * 再生プログラム。録画マクロ／ユニットを「1周分の線形な操作列＋繰り返し設定」に平坦化したもの。
 * 状態機械はこの共通表現だけを見て動くため、録画とユニットで再生ロジックを分岐しない。
 *
 * @param operations 1周分の操作列
 * @param repeatCount 再生回数。1=1周完走。0=無限。
 * @param loopWaitMs ループ間待機(ms)
 */
data class PlaybackProgram(
    val operations: List<PlannedOperation>,
    val repeatCount: Int,
    val loopWaitMs: Long,
) {
    val operationCount: Int get() = operations.size
    val isInfinite: Boolean get() = repeatCount == 0

    companion object {
        /**
         * 録画マクロから再生プログラムを作る。
         * @param repeatCount 再生設定（既定 1）
         * @param loopWaitMs 再生設定（既定 0）
         */
        fun fromRecording(
            recording: Recording,
            repeatCount: Int = 1,
            loopWaitMs: Long = 0L,
        ): PlaybackProgram {
            val ops = recording.operations
                .sortedBy { it.index }
                .map { PlannedOperation(it, it.waitAfterMs) }
            return PlaybackProgram(ops, repeatCount, loopWaitMs)
        }

        /**
         * ユニットから再生プログラムを作る。
         * 各要素の操作を連結し、「構成要素終了後の追加待機時間」は
         * その要素の最後の操作の待機時間へ加算する（仕様どおり）。
         */
        fun fromUnit(unit: MacroUnit): PlaybackProgram {
            val planned = mutableListOf<PlannedOperation>()
            for (element in unit.elements) {
                val ops = element.operations
                element.operations.forEachIndexed { i, op ->
                    val isLast = i == ops.lastIndex
                    val wait = if (isLast) op.waitAfterMs + element.extraWaitAfterMs else op.waitAfterMs
                    planned += PlannedOperation(op, wait)
                }
            }
            return PlaybackProgram(planned, unit.repeatCount, unit.loopWaitMs)
        }
    }
}

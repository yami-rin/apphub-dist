package com.wadop.touchmacro.core.unit

import com.wadop.touchmacro.core.model.MacroUnit
import com.wadop.touchmacro.core.model.Operation
import com.wadop.touchmacro.core.model.Recording
import com.wadop.touchmacro.core.model.UnitElement

/**
 * ユニット（合成マクロ）の生成・編集を行う純粋ロジック。
 *
 * 方針: ユニットはコピー方式。録画マクロを追加すると、その操作データを
 * 完全に複製して要素内へ格納する。以後、元の録画を編集・削除しても
 * 作成済みユニットには影響しない。
 *
 * すべての関数は入力を変更せず、新しいインスタンスを返す（イミュータブル）。
 *
 * @param idGenerator 要素ID・操作IDを生成する関数（テスト時は決定的な実装を渡せる）
 */
class UnitBuilder(
    private val idGenerator: () -> String,
) {

    /**
     * 録画マクロから、ユニット構成要素を「完全複製」して作る。
     * 操作データは deep copy し、操作IDも新規採番して元と独立させる。
     */
    fun elementFromRecording(
        recording: Recording,
        extraWaitAfterMs: Long = 0L,
    ): UnitElement {
        val copiedOps = recording.operations.map { op ->
            deepCopyOperation(op)
        }
        return UnitElement(
            elementId = idGenerator(),
            sourceRecordingId = recording.id,
            sourceDisplayName = recording.displayName,
            operations = copiedOps,
            extraWaitAfterMs = extraWaitAfterMs,
        )
    }

    /**
     * 操作を deep copy し、操作IDを新規に振り直す。
     * data class はデフォルトでは shallow copy になり得るため、
     * ネストしたリストも明示的に作り直して元マクロとの共有を断つ。
     */
    private fun deepCopyOperation(op: Operation): Operation {
        // payload 内の pointers/points は不変 data class の List だが、
        // 参照共有を避けるため新しい List に作り替える。
        val newPayload = when (val p = op.payload) {
            is com.wadop.touchmacro.core.model.CoordinatePayload ->
                com.wadop.touchmacro.core.model.CoordinatePayload(
                    pointers = p.pointers.map { trace ->
                        trace.copy(points = trace.points.map { it.copy() })
                    },
                )
        }
        return op.copy(id = idGenerator(), payload = newPayload)
    }

    /**
     * 新しいユニットを作成する。
     *
     * @param recordings 追加する録画マクロ列（同じマクロを複数回含めてよい）
     */
    fun createUnit(
        id: String,
        displayName: String,
        createdAtEpochMs: Long,
        recordings: List<Recording>,
        repeatCount: Int = 1,
        loopWaitMs: Long = 0L,
    ): MacroUnit {
        val elements = recordings.map { elementFromRecording(it) }
        return MacroUnit(
            id = id,
            displayName = displayName,
            createdAtEpochMs = createdAtEpochMs,
            elements = elements,
            repeatCount = repeatCount,
            loopWaitMs = loopWaitMs,
        )
    }

    /** 既存ユニットへ録画マクロを追加（末尾）。複製して追加する。 */
    fun addRecording(unit: MacroUnit, recording: Recording, extraWaitAfterMs: Long = 0L): MacroUnit =
        unit.copy(elements = unit.elements + elementFromRecording(recording, extraWaitAfterMs))

    /** 構成要素の順番を入れ替える（from を to の位置へ移動）。 */
    fun moveElement(unit: MacroUnit, from: Int, to: Int): MacroUnit {
        val list = unit.elements.toMutableList()
        require(from in list.indices) { "from が範囲外: $from" }
        require(to in list.indices) { "to が範囲外: $to" }
        val item = list.removeAt(from)
        list.add(to, item)
        return unit.copy(elements = list)
    }

    /** 構成要素を複製する（操作データも新IDで再複製し、直後に挿入）。 */
    fun duplicateElement(unit: MacroUnit, index: Int): MacroUnit {
        require(index in unit.elements.indices) { "index が範囲外: $index" }
        val src = unit.elements[index]
        val cloned = src.copy(
            elementId = idGenerator(),
            operations = src.operations.map { deepCopyOperation(it) },
        )
        val list = unit.elements.toMutableList()
        list.add(index + 1, cloned)
        return unit.copy(elements = list)
    }

    /** 構成要素を削除する。 */
    fun removeElement(unit: MacroUnit, index: Int): MacroUnit {
        require(index in unit.elements.indices) { "index が範囲外: $index" }
        return unit.copy(elements = unit.elements.filterIndexed { i, _ -> i != index })
    }

    /** 構成要素終了後の追加待機時間を個別変更する。 */
    fun changeExtraWait(unit: MacroUnit, index: Int, extraWaitAfterMs: Long): MacroUnit {
        require(index in unit.elements.indices) { "index が範囲外: $index" }
        require(extraWaitAfterMs >= 0) { "extraWaitAfterMs は非負: $extraWaitAfterMs" }
        val updated = unit.elements[index].copy(extraWaitAfterMs = extraWaitAfterMs)
        return unit.copy(elements = unit.elements.mapIndexed { i, e -> if (i == index) updated else e })
    }
}

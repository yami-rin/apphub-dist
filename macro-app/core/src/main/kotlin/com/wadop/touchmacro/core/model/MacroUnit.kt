package com.wadop.touchmacro.core.model

/**
 * ユニットの構成要素。追加された録画マクロの操作データを「完全に複製」して保持する。
 *
 * コピー方式のため、元の録画マクロを編集・削除しても本要素には一切影響しない。
 * （[sourceRecordingId] は由来の記録用であり、参照ではない）
 *
 * @param elementId ユニット内での一意ID（同じ録画を複数回追加できるため要素ごとに別ID）
 * @param sourceRecordingId 由来の録画マクロID。表示・トレーサビリティ用（実データは複製済み）。
 * @param sourceDisplayName 追加時点の元マクロ表示名のスナップショット。
 * @param operations 複製された操作データ（元マクロから完全コピー）
 * @param extraWaitAfterMs 構成要素終了後の追加待機時間。元マクロ内の待機へ加算される。
 */
data class UnitElement(
    val elementId: String,
    val sourceRecordingId: String?,
    val sourceDisplayName: String,
    val operations: List<Operation>,
    val extraWaitAfterMs: Long = 0L,
) {
    init {
        require(extraWaitAfterMs >= 0) { "extraWaitAfterMs は非負でなければならない: $extraWaitAfterMs" }
    }

    /** この要素単体の再生時間(ms)。操作継続＋操作間待機＋要素末尾の追加待機。 */
    val durationMs: Long
        get() = operations.sumOf { it.durationMs + it.waitAfterMs } + extraWaitAfterMs
}

/**
 * ユニット。複数の録画マクロを結合（コピー）して作った合成マクロ。
 *
 * @param id 一意ID
 * @param displayName 表示名
 * @param createdAtEpochMs 作成日時
 * @param elements 構成要素列（表示順）
 * @param repeatCount 再生回数。1=1周完走。0=無限。
 * @param loopWaitMs ループ間待機時間(ms)。初期0。
 * @param lastPlayedAtEpochMs 最終実行日時
 */
data class MacroUnit(
    override val id: String,
    override val displayName: String,
    override val createdAtEpochMs: Long,
    val elements: List<UnitElement>,
    val repeatCount: Int = 1,
    val loopWaitMs: Long = 0L,
    override val lastPlayedAtEpochMs: Long? = null,
) : MacroAsset {

    init {
        require(repeatCount >= 0) { "repeatCount は非負（0=無限）でなければならない: $repeatCount" }
        require(loopWaitMs >= 0) { "loopWaitMs は非負でなければならない: $loopWaitMs" }
    }

    override val kind: MacroKind get() = MacroKind.UNIT

    override val operationCount: Int get() = elements.sumOf { it.operations.size }

    override val totalDurationMs: Long
        get() = elements.sumOf { it.durationMs }
}

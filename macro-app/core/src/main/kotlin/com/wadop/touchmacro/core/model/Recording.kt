package com.wadop.touchmacro.core.model

/**
 * マクロ資産の種別。録画マクロとユニットを異なる種類として識別できるようにする。
 */
enum class MacroKind {
    /** ユーザーの操作を1回の録画で作成したマクロ。 */
    RECORDING,

    /** 複数の録画マクロを結合（コピー）して作成した合成マクロ。 */
    UNIT,
}

/**
 * 一覧表示や再生設定で、録画マクロ／ユニットを共通に扱うためのインターフェース。
 */
sealed interface MacroAsset {
    val id: String
    val displayName: String
    val createdAtEpochMs: Long
    val lastPlayedAtEpochMs: Long?
    val kind: MacroKind

    /** 総再生時間(ms)。全操作の継続時間＋操作間待機の合計（ループ・ループ間待機は含めない1周分）。 */
    val totalDurationMs: Long

    /** 操作数。 */
    val operationCount: Int
}

/**
 * 録画マクロ。ユーザーの物理タッチ操作を時刻付き座標列として保持する。
 *
 * @param id 一意ID
 * @param displayName 表示名。初期値は "YYYYMMDD-通番"。
 * @param createdAtEpochMs 作成日時
 * @param operations 操作列（index 昇順）
 * @param lastPlayedAtEpochMs 最終実行日時（未実行なら null）
 */
data class Recording(
    override val id: String,
    override val displayName: String,
    override val createdAtEpochMs: Long,
    val operations: List<Operation>,
    override val lastPlayedAtEpochMs: Long? = null,
) : MacroAsset {

    override val kind: MacroKind get() = MacroKind.RECORDING

    override val operationCount: Int get() = operations.size

    override val totalDurationMs: Long
        get() = operations.sumOf { it.durationMs + it.waitAfterMs }
}

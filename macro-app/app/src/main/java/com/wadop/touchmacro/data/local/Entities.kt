package com.wadop.touchmacro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 録画マクロの永続化エンティティ。
 * 操作列は JSON 文字列（operationsJson）として保存する。
 * 将来のスキーマ変更に備え、操作データはカラム分割せず1列にまとめている。
 */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val createdAtEpochMs: Long,
    val lastPlayedAtEpochMs: Long?,
    /** 再生設定：再生回数（1=1周, 0=無限）。 */
    val repeatCount: Int,
    /** 再生設定：ループ間待機(ms)。 */
    val loopWaitMs: Long,
    /** 一時保存（画面消灯/異常終了時の復旧用）フラグ。 */
    val isDraft: Boolean,
    val operationsJson: String,
)

/**
 * ユニット（合成マクロ）の永続化エンティティ。
 * 構成要素（コピー済み操作データを含む）は JSON 文字列で保存する。
 */
@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val createdAtEpochMs: Long,
    val lastPlayedAtEpochMs: Long?,
    val repeatCount: Int,
    val loopWaitMs: Long,
    val elementsJson: String,
)

/**
 * 日付ごとの通番カウンタ。
 * 「削除された番号は再利用しない」ため、発行済みの最大通番を保持する（削除では減らさない）。
 */
@Entity(tableName = "sequence_counters")
data class SequenceCounterEntity(
    @PrimaryKey val dateKey: String,
    val lastIssuedSeq: Int,
)

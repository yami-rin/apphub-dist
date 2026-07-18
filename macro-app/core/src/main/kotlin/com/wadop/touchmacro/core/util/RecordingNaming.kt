package com.wadop.touchmacro.core.util

/**
 * 録画の初期名称 "YYYYMMDD-通番" の採番ロジック。
 *
 * 仕様:
 * - 通番は日付ごとに 1 から開始する。
 * - 削除された番号は再利用しない。
 *
 * 「再利用しない」を満たすため、現存件数ではなく
 * 「その日にこれまで発行した通番の最大値」を根拠に次番号を決める。
 * 発行済み最大値は永続化して保持する（削除では減らさない）。
 */
object RecordingNaming {

    /**
     * 次の初期名称を生成する。
     *
     * @param dateKey "YYYYMMDD" 形式の日付キー
     * @param lastIssuedSeqForDate その日にこれまで発行した通番の最大値。未発行なら 0。
     * @return 生成された名称と、新たな発行済み最大値のペア
     */
    fun nextName(dateKey: String, lastIssuedSeqForDate: Int): NameResult {
        require(dateKey.length == 8 && dateKey.all { it.isDigit() }) {
            "dateKey は YYYYMMDD 形式でなければならない: $dateKey"
        }
        require(lastIssuedSeqForDate >= 0) { "lastIssuedSeqForDate は非負: $lastIssuedSeqForDate" }
        val seq = lastIssuedSeqForDate + 1
        return NameResult(name = "$dateKey-$seq", issuedSeq = seq)
    }

    data class NameResult(val name: String, val issuedSeq: Int)
}

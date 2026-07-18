package com.wadop.touchmacro.core.playback

/**
 * 繰り返し（ループ）計算を担う。純粋な状態オブジェクト。
 *
 * 仕様:
 * - 初期再生回数は1回。1回=先頭から末尾までの完走。
 * - 再生回数が0の場合は無限再生。
 * - ループ間待機は、1回の再生終了後〜次の再生開始前に適用。
 * - 最初の再生前には待たない。指定された最終回の終了後には待たない。
 * - 一時停止中はループ回数を進めない（onLoopCompleted を呼ばないことで保証）。
 * - 途中終了/中断されたループは完了回数に数えない（同上）。
 *
 * @param targetLoops 目標再生回数。0=無限。
 */
class LoopController(
    val targetLoops: Int,
) {
    init {
        require(targetLoops >= 0) { "targetLoops は非負（0=無限）: $targetLoops" }
    }

    /** 完了したループ数（完走したものだけ数える）。 */
    var completedLoops: Int = 0
        private set

    val isInfinite: Boolean get() = targetLoops == 0

    /** まだ実行すべきループが残っているか（現在の完了数を基準に判定）。 */
    fun hasMoreLoops(): Boolean = isInfinite || completedLoops < targetLoops

    /**
     * 1ループを完走したときに呼ぶ。完了数を1増やす。
     * 一時停止・中断時には呼ばないこと（=回数を進めない）。
     */
    fun onLoopCompleted() {
        completedLoops++
    }

    /**
     * 直前のループ完了後、次のループへ進む前に「ループ間待機」を挟むべきか。
     *
     * 最終回の終了後には待たない＝「次のループが実際に走る場合のみ待つ」。
     * onLoopCompleted() 済みの状態で呼ぶ想定。
     */
    fun shouldWaitBeforeNextLoop(): Boolean = hasMoreLoops()

    /** 全ループが完了して再生を終えてよいか。 */
    fun isFinished(): Boolean = !hasMoreLoops()
}

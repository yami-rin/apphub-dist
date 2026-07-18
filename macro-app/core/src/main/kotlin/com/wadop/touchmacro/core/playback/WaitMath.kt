package com.wadop.touchmacro.core.playback

/**
 * 待機時間の残り計算。一時停止時の残り待機時間を扱う。
 *
 * 仕様:
 * - 待機中に一時停止した場合は、残り待機時間を保存する。
 * - 再開後は残り待機時間の経過後に次の操作へ進む。
 */
object WaitMath {

    /**
     * 残り待機時間を求める。
     *
     * @param totalWaitMs 元の待機時間
     * @param alreadyElapsedMs 一時停止までに経過した待機時間
     * @return 残り待機時間（0..totalWaitMs にクランプ）
     */
    fun remaining(totalWaitMs: Long, alreadyElapsedMs: Long): Long {
        require(totalWaitMs >= 0) { "totalWaitMs は非負: $totalWaitMs" }
        val elapsed = alreadyElapsedMs.coerceAtLeast(0)
        return (totalWaitMs - elapsed).coerceIn(0, totalWaitMs)
    }
}

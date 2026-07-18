package com.wadop.touchmacro.core.playback

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 一時停止時の残り待機時間計算テスト。
 */
class WaitMathTest {

    @Test
    fun `待機途中で一時停止した場合の残り時間`() {
        // 総待機1000ms、300ms経過して一時停止 → 残り700ms
        assertEquals(700L, WaitMath.remaining(totalWaitMs = 1000, alreadyElapsedMs = 300))
    }

    @Test
    fun `経過が総待機を超えても0でクランプ`() {
        assertEquals(0L, WaitMath.remaining(totalWaitMs = 1000, alreadyElapsedMs = 1500))
    }

    @Test
    fun `経過0なら残りは総待機のまま`() {
        assertEquals(1000L, WaitMath.remaining(totalWaitMs = 1000, alreadyElapsedMs = 0))
    }

    @Test
    fun `負の経過は0扱い`() {
        assertEquals(1000L, WaitMath.remaining(totalWaitMs = 1000, alreadyElapsedMs = -50))
    }
}

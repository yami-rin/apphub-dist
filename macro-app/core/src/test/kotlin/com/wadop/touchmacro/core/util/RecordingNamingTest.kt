package com.wadop.touchmacro.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 録画初期名称の採番テスト。
 */
class RecordingNamingTest {

    @Test
    fun `通番は日付ごとに1から開始する`() {
        val r = RecordingNaming.nextName("20260718", lastIssuedSeqForDate = 0)
        assertEquals("20260718-1", r.name)
        assertEquals(1, r.issuedSeq)
    }

    @Test
    fun `削除された番号は再利用しない`() {
        // 3件発行済み（最大3）。1件削除しても、次は最大値+1=4 を採番する。
        val r = RecordingNaming.nextName("20260718", lastIssuedSeqForDate = 3)
        assertEquals("20260718-4", r.name)
        assertEquals(4, r.issuedSeq)
    }

    @Test
    fun `日付が変われば通番はリセットされる`() {
        val r = RecordingNaming.nextName("20260719", lastIssuedSeqForDate = 0)
        assertEquals("20260719-1", r.name)
    }
}

package com.wadop.touchmacro.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 表示用の整形ヘルパ。 */
object Formatters {

    private val dateTimeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
    private val dateKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.JAPAN)

    fun dateTime(epochMs: Long?): String =
        if (epochMs == null) "未実行" else dateTimeFormat.format(Date(epochMs))

    /** "YYYYMMDD" の日付キー。通番採番に使う。 */
    fun dateKey(epochMs: Long): String = dateKeyFormat.format(Date(epochMs))

    /** ミリ秒を "m分s.S秒" 風に整形。 */
    fun duration(ms: Long): String {
        val totalSec = ms / 1000.0
        return if (totalSec >= 60) {
            val m = (ms / 60000)
            val s = (ms % 60000) / 1000.0
            "%d分%.1f秒".format(m, s)
        } else {
            "%.1f秒".format(totalSec)
        }
    }
}

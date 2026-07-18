package com.wadop.touchmacro.service

import android.content.Context
import android.widget.Toast
import com.wadop.touchmacro.core.model.MacroUnit
import com.wadop.touchmacro.core.model.Recording
import com.wadop.touchmacro.core.playback.PlaybackProgram
import com.wadop.touchmacro.core.playback.PlaybackState
import com.wadop.touchmacro.util.PermissionChecker

/**
 * 一覧/設定画面から再生を起動するための橋渡し。
 * 権限確認とサービス接続確認を行い、AccessibilityService へ再生を依頼する。
 */
object PlaybackLauncher {

    /**
     * 録画マクロを再生する。
     * @return 起動できたら true。権限不足やサービス未接続なら false。
     */
    fun playRecording(
        context: Context,
        recording: Recording,
        repeatCount: Int,
        loopWaitMs: Long,
        onFinished: (PlaybackState) -> Unit = {},
    ): Boolean {
        val program = PlaybackProgram.fromRecording(recording, repeatCount, loopWaitMs)
        return play(context, program, onFinished)
    }

    /** ユニットを再生する。 */
    fun playUnit(
        context: Context,
        unit: MacroUnit,
        onFinished: (PlaybackState) -> Unit = {},
    ): Boolean {
        val program = PlaybackProgram.fromUnit(unit)
        return play(context, program, onFinished)
    }

    private fun play(
        context: Context,
        program: PlaybackProgram,
        onFinished: (PlaybackState) -> Unit,
    ): Boolean {
        if (!PermissionChecker.hasRequiredForPlayback(context)) {
            Toast.makeText(context, "権限が不足しています。設定を確認してください。", Toast.LENGTH_LONG).show()
            return false
        }
        val service = TouchMacroAccessibilityService.instance
        if (service == null) {
            Toast.makeText(context, "ユーザー補助サービスが接続されていません。", Toast.LENGTH_LONG).show()
            return false
        }
        service.startPlayback(program, onFinished)
        return true
    }
}

package com.wadop.touchmacro.core.playback

/**
 * 再生状態機械の状態。仕様で列挙された10状態を明示的に表現する。
 *
 * 各状態は再生カーソル（何周目・何番目の操作か）や残り待機時間を保持し、
 * 一時停止→再開時に正しく再開位置を復元できるようにする。
 */
sealed interface PlaybackState {

    /** 待機（未開始）。 */
    data object Idle : PlaybackState

    /** 準備中（権限確認・オーバーレイ準備など）。 */
    data object Preparing : PlaybackState

    /**
     * 操作を実行中（ジェスチャー送信中）。
     * @param opIndex 1周内の操作インデックス(0始まり)
     * @param loop 現在の周回(1始まり)
     */
    data class Playing(val opIndex: Int, val loop: Int) : PlaybackState

    /**
     * 操作間の待機中。
     * @param afterOpIndex 直前に完了した操作インデックス
     * @param loop 現在の周回
     * @param remainingWaitMs 残り待機時間
     */
    data class WaitingBetweenOperations(
        val afterOpIndex: Int,
        val loop: Int,
        val remainingWaitMs: Long,
    ) : PlaybackState

    /**
     * ループ間の待機中。
     * @param nextLoop 次に開始する周回(1始まり)
     * @param remainingWaitMs 残り待機時間
     */
    data class WaitingBetweenLoops(
        val nextLoop: Int,
        val remainingWaitMs: Long,
    ) : PlaybackState

    /**
     * 操作の実行途中で一時停止した。中断した操作は再実行せず、再開後は次の操作へ進む。
     */
    data class PausedDuringGesture(val opIndex: Int, val loop: Int) : PlaybackState

    /**
     * 待機中に一時停止した。残り待機時間を保持し、再開後にその経過後、次へ進む。
     * @param resume 再開後に向かう先（次操作 or 次ループ）
     */
    data class PausedDuringWait(
        val remainingWaitMs: Long,
        val resume: WaitResume,
    ) : PlaybackState

    /** 全周回を完走して正常終了。 */
    data object Completed : PlaybackState

    /** ユーザー操作やサービス停止等で終了。 */
    data object Stopped : PlaybackState

    /** エラーで停止（ジェスチャーキャンセル、権限不足、データ破損など）。 */
    data class Error(val reason: String) : PlaybackState
}

/**
 * 待機からの再開先。
 */
sealed interface WaitResume {
    /** 操作間待機からの再開：afterOpIndex の次の操作へ進む。 */
    data class ToNextOperation(val afterOpIndex: Int, val loop: Int) : WaitResume

    /** ループ間待機からの再開：nextLoop の先頭操作へ進む。 */
    data class ToNextLoop(val nextLoop: Int) : WaitResume
}

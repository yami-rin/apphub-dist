package com.wadop.touchmacro.core.model

/**
 * 1本の指の、ある瞬間の座標サンプル。
 *
 * @param tOffsetMs 操作開始（最初の指が触れた瞬間）からの相対時刻(ms)
 * @param x 端末スクリーン座標(px)
 * @param y 端末スクリーン座標(px)
 * @param pressure 筆圧(0..1)。取得できない端末では 1f を入れる。将来拡張用。
 */
data class TracePoint(
    val tOffsetMs: Long,
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
)

/**
 * 1本の指（Pointer）の時刻付き座標列。
 * タップ／長押し／スワイプ／ドラッグ／ピンチの区別は持たず、
 * 「点が1つだけ＝タップ相当」「点が複数＝軌跡あり」という共通表現にする。
 *
 * @param pointerId 録画時の MotionEvent Pointer ID
 * @param points 時系列に並んだ座標サンプル（tOffsetMs 昇順）
 */
data class PointerTrace(
    val pointerId: Int,
    val points: List<TracePoint>,
) {
    /** この指が接地していた時間(ms)。 */
    val durationMs: Long
        get() = if (points.isEmpty()) 0L else points.last().tOffsetMs - points.first().tOffsetMs
}

/**
 * 操作のペイロード（実体データ）。
 *
 * 現在は座標ジェスチャー([CoordinatePayload])のみだが、
 * 将来 UI 要素ベースの記録方式（例: 特定ボタンのクリック）を
 * 追加できるよう sealed interface で拡張点を確保している。
 */
sealed interface OperationPayload {
    /** ペイロード種別の識別子。データ永続化時のタグに使う。 */
    val kind: String
}

/**
 * 座標ジェスチャーのペイロード。複数指の軌跡をそのまま保持する。
 * タップ・スワイプ・ピンチ・多指操作をすべてこの共通形式で表現する。
 */
data class CoordinatePayload(
    val pointers: List<PointerTrace>,
) : OperationPayload {
    override val kind: String get() = KIND

    /** 指の本数。 */
    val pointerCount: Int get() = pointers.size

    companion object {
        const val KIND = "coordinate"
    }
}

/** 操作の完了状態。ジェスチャー送信が完了したか、途中で中断/キャンセルされたか。 */
enum class OperationStatus {
    /** 正常に最後まで実行/記録された。 */
    COMPLETED,

    /** ユーザー操作や画面消灯等で途中中断された（再実行しない）。 */
    CANCELLED,
}

/**
 * 1操作。最初の指が画面に触れてから、すべての指が離れるまでを1操作とする。
 *
 * タップ／スワイプ／ピンチを別形式にせず、共通の複数指軌跡データとして保存する。
 *
 * @param id 操作ID（マクロ内で一意）
 * @param index 録画内での0始まりの並び順
 * @param startedAtEpochMs 操作開始時刻（実時刻, epoch ms）
 * @param durationMs 操作継続時間（最初の接地〜全指解放まで）
 * @param elapsedFromStartMs 録画開始からの経過時間（この操作開始まで）
 * @param status 完了またはキャンセル状態
 * @param waitAfterMs 次の操作までの待機時間（無操作時間）。再生時に再現する。
 * @param payload 操作の実体。座標ジェスチャー等。
 */
data class Operation(
    val id: String,
    val index: Int,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val elapsedFromStartMs: Long,
    val status: OperationStatus,
    val waitAfterMs: Long,
    val payload: OperationPayload,
) {
    init {
        require(durationMs >= 0) { "durationMs は非負でなければならない: $durationMs" }
        require(waitAfterMs >= 0) { "waitAfterMs は非負でなければならない: $waitAfterMs" }
    }
}

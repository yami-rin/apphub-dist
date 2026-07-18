package com.wadop.touchmacro.service.gesture

import android.accessibilityservice.GestureDescription

/**
 * ジェスチャー送信の抽象。AccessibilityService が実装する。
 * PlaybackController を Android サービスから疎結合にしてテストしやすくする。
 */
interface GestureDispatcher {
    /**
     * ジェスチャーを送信する。
     * @return 送信要求が受理されたら true
     */
    fun dispatch(
        gesture: GestureDescription,
        onCompleted: () -> Unit,
        onCancelled: () -> Unit,
    ): Boolean
}

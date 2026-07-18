package com.wadop.touchmacro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

/**
 * アプリのエントリポイント。Hilt の DI コンテナを初期化する。
 */
@HiltAndroidApp
class TouchMacroApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createSessionNotificationChannel()
    }

    /** 録画・再生の常駐通知用チャンネルを作成する。 */
    private fun createSessionNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_SESSION,
            getString(R.string.notif_channel_session),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_SESSION = "macro_session"
    }
}

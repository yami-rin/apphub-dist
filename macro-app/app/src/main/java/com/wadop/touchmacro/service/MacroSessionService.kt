package com.wadop.touchmacro.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wadop.touchmacro.R
import com.wadop.touchmacro.TouchMacroApp

/**
 * 録画/再生中に常駐するフォアグラウンドサービス。
 * OS によるプロセス回収を避け、セッションの継続と状態通知を担う。
 */
class MacroSessionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_PLAYING
        startForegroundCompat(mode)
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(mode: String) {
        val text = if (mode == MODE_RECORDING) {
            getString(R.string.notif_recording)
        } else {
            getString(R.string.notif_playing)
        }
        val notification: Notification = NotificationCompat.Builder(this, TouchMacroApp.CHANNEL_SESSION)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_RECORDING = "recording"
        const val MODE_PLAYING = "playing"
        private const val NOTIF_ID = 1001
    }
}

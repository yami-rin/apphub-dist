package com.wadop.touchmacro.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.wadop.touchmacro.service.TouchMacroAccessibilityService

/**
 * 権限・設定状態の確認と、各設定画面への遷移を提供する。
 *
 * 本アプリが必要とする権限:
 * - ユーザー補助サービス（再生の dispatchGesture に必須）
 * - 他のアプリ上への表示（オーバーレイ）
 * - 通知（Android 13+）
 * - バッテリー最適化除外（任意・常駐安定のため）
 */
object PermissionChecker {

    /** ユーザー補助サービスが有効か。 */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(
            context,
            TouchMacroAccessibilityService::class.java,
        ).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        for (name in splitter) {
            if (name.equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    /** オーバーレイ表示権限があるか。 */
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** 通知権限があるか（13未満は常に true）。 */
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /** バッテリー最適化が除外されているか。 */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** 再生に必須の権限（ユーザー補助＋オーバーレイ）が揃っているか。 */
    fun hasRequiredForPlayback(context: Context): Boolean =
        isAccessibilityEnabled(context) && canDrawOverlays(context)

    // ---- 設定画面への遷移 Intent ----

    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun batteryOptimizationSettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

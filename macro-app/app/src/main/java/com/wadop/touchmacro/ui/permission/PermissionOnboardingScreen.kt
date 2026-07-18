package com.wadop.touchmacro.ui.permission

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import android.os.Build
import com.wadop.touchmacro.util.PermissionChecker

/**
 * 権限案内画面。必要な権限が不足している場合は録画/再生を開始せず、
 * 各設定画面へ誘導する。画面復帰時に状態を再チェックする。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // onResume ごとに再チェックするためのトリガ。
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshKey++ }

    // refreshKey を参照して再評価
    @Suppress("UNUSED_EXPRESSION")
    refreshKey

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("初期設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "マクロの記録・再生には以下の権限が必要です。不足している場合は各設定を開いて許可してください。",
                style = MaterialTheme.typography.bodyMedium,
            )

            PermissionRow(
                title = "ユーザー補助サービス（必須）",
                granted = PermissionChecker.isAccessibilityEnabled(context),
                description = "dispatchGesture による再生に必須です。",
                onOpen = { context.startActivity(PermissionChecker.accessibilitySettingsIntent()) },
            )
            PermissionRow(
                title = "他のアプリ上への表示（必須）",
                granted = PermissionChecker.canDrawOverlays(context),
                description = "録画/再生の操作パネル表示に必要です。",
                onOpen = { context.startActivity(PermissionChecker.overlaySettingsIntent(context)) },
            )
            PermissionRow(
                title = "通知",
                granted = PermissionChecker.hasNotificationPermission(context),
                description = "録画/再生中の状態表示に使用します。",
                onOpen = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
            PermissionRow(
                title = "バッテリー最適化の除外（任意）",
                granted = PermissionChecker.isIgnoringBatteryOptimizations(context),
                description = "長時間の常駐を安定させます。",
                onOpen = { context.startActivity(PermissionChecker.batteryOptimizationSettingsIntent()) },
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    description: String,
    onOpen: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (granted) Color(0xFF2E7D32) else Color(0xFFEF6C00),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            if (!granted) {
                Button(onClick = onOpen) { Text("設定") }
            }
        }
    }
}

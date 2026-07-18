package com.wadop.touchmacro.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wadop.touchmacro.util.PermissionChecker

/**
 * 録画（技術検証プロトタイプ）画面。
 * 公開APIの制約を明示したうえで、オーバーレイ面上のタッチ取得を試せる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onBack: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val canOverlay = PermissionChecker.canDrawOverlays(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("録画（技術検証）") },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("重要な制約", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "非root・公開APIのみでは、他アプリ上の物理タッチを『妨げずに』記録することはできません。" +
                            "本プロトタイプは全画面の透過ビューでタッチを取得しますが、その間タッチは配下アプリへ届きません。" +
                            "取得可能性の検証用としてご利用ください。詳細は README を参照。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (!canOverlay) {
                Text("オーバーレイ権限が必要です。初期設定から許可してください。")
            }

            if (!state.recording) {
                Button(
                    onClick = { viewModel.start() },
                    enabled = canOverlay,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("録画開始（プロトタイプ）") }
            } else {
                Button(
                    onClick = { viewModel.stopAndSave() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("保存して終了") }
                OutlinedButton(
                    onClick = { viewModel.stopWithoutSaving() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("保存せず終了") }
            }

            state.savedName?.let {
                Text("保存しました: $it（取得操作数 ${state.capturedCount}）")
            }
        }
    }
}

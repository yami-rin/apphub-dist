package com.wadop.touchmacro.ui.unit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * ユニット作成画面。録画マクロを追加（コピー）して並べ替え・複製・削除・追加待機を編集する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitEditorScreen(
    onBack: () -> Unit,
    viewModel: UnitEditorViewModel = hiltViewModel(),
) {
    val unit by viewModel.unit.collectAsStateWithLifecycle()
    val available by viewModel.availableRecordings.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ユニット作成") },
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = unit.displayName,
                onValueChange = viewModel::setDisplayName,
                label = { Text("表示名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = unit.repeatCount.toString(),
                    onValueChange = { viewModel.setRepeatCount(it.toIntOrNull() ?: 0) },
                    label = { Text("再生回数(0=無限)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = (unit.loopWaitMs / 1000.0).toString(),
                    onValueChange = { viewModel.setLoopWaitMs(((it.toDoubleOrNull() ?: 0.0) * 1000).toLong()) },
                    label = { Text("ループ間待機(秒)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("録画マクロを追加")
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(unit.elements, key = { _, e -> e.elementId }) { index, element ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${index + 1}. ${element.sourceDisplayName}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "操作数 ${element.operations.size}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            OutlinedTextField(
                                value = (element.extraWaitAfterMs / 1000.0).toString(),
                                onValueChange = {
                                    viewModel.changeExtraWaitMs(index, ((it.toDoubleOrNull() ?: 0.0) * 1000).toLong())
                                },
                                label = { Text("終了後の追加待機(秒)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = { viewModel.moveUp(index) }) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上へ")
                                }
                                IconButton(onClick = { viewModel.moveDown(index) }) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下へ")
                                }
                                Text(
                                    "複製",
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .weight(1f),
                                )
                                OutlinedButton(onClick = { viewModel.duplicate(index) }) { Text("複製") }
                                OutlinedButton(onClick = { viewModel.remove(index) }) { Text("削除") }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.save(onBack) },
                enabled = unit.elements.isNotEmpty() && unit.displayName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ユニットを保存") }
        }
    }

    if (showPicker) {
        RecordingPickerDialog(
            recordings = available,
            onPick = { viewModel.addRecording(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

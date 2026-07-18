package com.wadop.touchmacro.ui.unit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wadop.touchmacro.core.model.Recording

/** ユニットへ追加する録画マクロを選ぶダイアログ。同じマクロを複数回追加できる。 */
@Composable
fun RecordingPickerDialog(
    recordings: List<Recording>,
    onPick: (Recording) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("録画マクロを選択") },
        text = {
            if (recordings.isEmpty()) {
                Text("追加できる録画マクロがありません。")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(recordings, key = { it.id }) { rec ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(rec) }
                                .padding(vertical = 12.dp),
                        ) {
                            Text(rec.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "操作数 ${rec.operationCount}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
    )
}

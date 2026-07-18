package com.wadop.touchmacro.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wadop.touchmacro.core.model.MacroKind
import com.wadop.touchmacro.util.Formatters

/**
 * マクロ一覧画面。録画マクロとユニットを種別バッジ付きで表示し、
 * 再生・名称変更・複製・削除・再生設定・ユニット作成への導線を提供する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroListScreen(
    onOpenSettings: (MacroListItem) -> Unit,
    onCreateUnit: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenRecord: () -> Unit,
    viewModel: MacroListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("マクロ一覧") },
                actions = {
                    IconButton(onClick = onOpenRecord) {
                        Icon(Icons.Filled.Add, contentDescription = "録画")
                    }
                    IconButton(onClick = onOpenPermissions) {
                        Icon(Icons.Filled.Settings, contentDescription = "権限設定")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("ユニット作成") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onCreateUnit,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.id }) { item ->
                MacroCard(
                    item = item,
                    onPlay = { viewModel.play(item) },
                    onRename = { viewModel.rename(item, it) },
                    onDuplicate = { viewModel.duplicate(item) },
                    onDelete = { viewModel.delete(item) },
                    onSettings = { onOpenSettings(item) },
                )
            }
        }
    }
}

@Composable
private fun MacroCard(
    item: MacroListItem,
    onPlay: () -> Unit,
    onRename: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                KindBadge(item.kind)
                Text(
                    text = "操作数 ${item.operationCount} ・ 総再生 ${Formatters.duration(item.totalDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "作成 ${Formatters.dateTime(item.createdAtEpochMs)} ・ 最終実行 ${Formatters.dateTime(item.lastPlayedAtEpochMs)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "再生")
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "メニュー")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("再生設定") }, onClick = { menuOpen = false; onSettings() })
                DropdownMenuItem(text = { Text("名称変更") }, onClick = { menuOpen = false; renaming = true })
                DropdownMenuItem(text = { Text("複製") }, onClick = { menuOpen = false; onDuplicate() })
                DropdownMenuItem(text = { Text("削除") }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }

    if (renaming) {
        RenameDialog(
            initial = item.displayName,
            onConfirm = { renaming = false; onRename(it) },
            onDismiss = { renaming = false },
        )
    }
}

@Composable
private fun KindBadge(kind: MacroKind) {
    val label = if (kind == MacroKind.UNIT) "ユニット" else "録画"
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(),
    )
}

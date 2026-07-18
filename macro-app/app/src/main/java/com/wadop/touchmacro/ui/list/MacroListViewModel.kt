package com.wadop.touchmacro.ui.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wadop.touchmacro.core.model.MacroKind
import com.wadop.touchmacro.domain.repository.MacroRepository
import com.wadop.touchmacro.service.PlaybackLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 一覧表示用のUIモデル。録画マクロとユニットを共通に扱う。
 */
data class MacroListItem(
    val id: String,
    val displayName: String,
    val kind: MacroKind,
    val createdAtEpochMs: Long,
    val totalDurationMs: Long,
    val operationCount: Int,
    val lastPlayedAtEpochMs: Long?,
)

@HiltViewModel
class MacroListViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: MacroRepository,
) : ViewModel() {

    /** 録画マクロとユニットを結合した一覧。作成日時の新しい順。 */
    val items: StateFlow<List<MacroListItem>> =
        combine(
            repository.observeRecordings(),
            repository.observeUnits(),
        ) { recordings, units ->
            val recItems = recordings.map {
                MacroListItem(
                    it.id, it.displayName, MacroKind.RECORDING,
                    it.createdAtEpochMs, it.totalDurationMs, it.operationCount, it.lastPlayedAtEpochMs,
                )
            }
            val unitItems = units.map {
                MacroListItem(
                    it.id, it.displayName, MacroKind.UNIT,
                    it.createdAtEpochMs, it.totalDurationMs, it.operationCount, it.lastPlayedAtEpochMs,
                )
            }
            (recItems + unitItems).sortedByDescending { it.createdAtEpochMs }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun rename(item: MacroListItem, newName: String) = viewModelScope.launch {
        when (item.kind) {
            MacroKind.RECORDING -> repository.renameRecording(item.id, newName)
            MacroKind.UNIT -> repository.renameUnit(item.id, newName)
        }
    }

    fun delete(item: MacroListItem) = viewModelScope.launch {
        when (item.kind) {
            MacroKind.RECORDING -> repository.deleteRecording(item.id)
            MacroKind.UNIT -> repository.deleteUnit(item.id)
        }
    }

    /** マクロ/ユニットを再生する。権限確認とサービス接続確認は Launcher が行う。 */
    fun play(item: MacroListItem) = viewModelScope.launch {
        when (item.kind) {
            MacroKind.RECORDING -> {
                val rec = repository.getRecording(item.id) ?: return@launch
                val (repeat, loopWait) = repository.getRecordingPlaybackSettings(item.id)
                val started = PlaybackLauncher.playRecording(appContext, rec, repeat, loopWait)
                if (started) repository.updateRecordingLastPlayed(item.id, System.currentTimeMillis())
            }
            MacroKind.UNIT -> {
                val unit = repository.getUnit(item.id) ?: return@launch
                val started = PlaybackLauncher.playUnit(appContext, unit)
                if (started) repository.updateUnitLastPlayed(item.id, System.currentTimeMillis())
            }
        }
    }

    fun duplicate(item: MacroListItem) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        when (item.kind) {
            MacroKind.RECORDING ->
                repository.duplicateRecording(
                    id = item.id,
                    newId = UUID.randomUUID().toString(),
                    newName = "${item.displayName} のコピー",
                    now = now,
                )
            MacroKind.UNIT -> {
                val unit = repository.getUnit(item.id) ?: return@launch
                repository.saveUnit(
                    unit.copy(
                        id = UUID.randomUUID().toString(),
                        displayName = "${item.displayName} のコピー",
                        createdAtEpochMs = now,
                        lastPlayedAtEpochMs = null,
                    ),
                )
            }
        }
    }
}

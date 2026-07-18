package com.wadop.touchmacro.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wadop.touchmacro.core.model.MacroKind
import com.wadop.touchmacro.domain.repository.MacroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaybackSettingsState(
    val displayName: String = "",
    val repeatCount: Int = 1,
    val loopWaitMs: Long = 0L,
    val loaded: Boolean = false,
)

/**
 * 再生設定（再生回数・ループ間待機）の編集。録画マクロ／ユニット双方に対応。
 * 再生回数 0 は無限再生。
 */
@HiltViewModel
class PlaybackSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MacroRepository,
) : ViewModel() {

    private val macroId: String = checkNotNull(savedStateHandle["macroId"])
    private val kind: MacroKind =
        MacroKind.valueOf(savedStateHandle["kind"] ?: MacroKind.RECORDING.name)

    private val _state = MutableStateFlow(PlaybackSettingsState())
    val state: StateFlow<PlaybackSettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (kind) {
                MacroKind.RECORDING -> {
                    val rec = repository.getRecording(macroId)
                    val (repeat, loopWait) = repository.getRecordingPlaybackSettings(macroId)
                    _state.value = PlaybackSettingsState(
                        displayName = rec?.displayName ?: "",
                        repeatCount = repeat,
                        loopWaitMs = loopWait,
                        loaded = true,
                    )
                }
                MacroKind.UNIT -> {
                    val unit = repository.getUnit(macroId)
                    _state.value = PlaybackSettingsState(
                        displayName = unit?.displayName ?: "",
                        repeatCount = unit?.repeatCount ?: 1,
                        loopWaitMs = unit?.loopWaitMs ?: 0L,
                        loaded = true,
                    )
                }
            }
        }
    }

    fun setRepeatCount(value: Int) {
        _state.value = _state.value.copy(repeatCount = value.coerceAtLeast(0))
    }

    fun setLoopWaitMs(value: Long) {
        _state.value = _state.value.copy(loopWaitMs = value.coerceAtLeast(0))
    }

    fun save(onSaved: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        when (kind) {
            MacroKind.RECORDING ->
                repository.updateRecordingPlaybackSettings(macroId, s.repeatCount, s.loopWaitMs)
            MacroKind.UNIT -> {
                val unit = repository.getUnit(macroId) ?: return@launch
                repository.saveUnit(unit.copy(repeatCount = s.repeatCount, loopWaitMs = s.loopWaitMs))
            }
        }
        onSaved()
    }
}

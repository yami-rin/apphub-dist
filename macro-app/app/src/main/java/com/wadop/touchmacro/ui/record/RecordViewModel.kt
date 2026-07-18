package com.wadop.touchmacro.ui.record

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wadop.touchmacro.core.model.Operation
import com.wadop.touchmacro.domain.repository.MacroRepository
import com.wadop.touchmacro.service.RecorderPrototype
import com.wadop.touchmacro.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordUiState(
    val recording: Boolean = false,
    val capturedCount: Int = 0,
    val savedName: String? = null,
)

/**
 * 【技術検証】録画プロトタイプの制御。
 * RecorderPrototype でオーバーレイ面上のタッチを取得し、[Operation] として保存する。
 * 「配下アプリを妨げずに記録」は公開APIでは不可能なため、本機能は取得可能性の検証用。
 */
@HiltViewModel
class RecordViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: MacroRepository,
) : ViewModel() {

    private val prototype = RecorderPrototype(appContext)

    private val _state = MutableStateFlow(RecordUiState())
    val state: StateFlow<RecordUiState> = _state.asStateFlow()

    fun start() {
        prototype.start()
        _state.value = RecordUiState(recording = true)
    }

    fun stopAndSave() {
        prototype.stop()
        val ops: List<Operation> = prototype.recordedOperations.toList()
        _state.value = _state.value.copy(recording = false, capturedCount = ops.size)
        if (ops.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val rec = repository.saveRecording(
                operations = ops,
                createdAtEpochMs = now,
                dateKey = Formatters.dateKey(now),
            )
            _state.value = _state.value.copy(savedName = rec.displayName)
        }
    }

    fun stopWithoutSaving() {
        prototype.stop()
        _state.value = _state.value.copy(recording = false)
    }

    override fun onCleared() {
        prototype.stop()
        super.onCleared()
    }
}

package com.wadop.touchmacro.ui.unit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wadop.touchmacro.core.model.MacroUnit
import com.wadop.touchmacro.core.model.Recording
import com.wadop.touchmacro.core.unit.UnitBuilder
import com.wadop.touchmacro.domain.repository.MacroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ユニット作成画面のロジック。
 *
 * 追加された録画マクロは [UnitBuilder] によりコピー方式で複製される。
 * 元マクロを後で編集・削除しても、作成済みユニットには影響しない。
 */
@HiltViewModel
class UnitEditorViewModel @Inject constructor(
    private val repository: MacroRepository,
) : ViewModel() {

    private val builder = UnitBuilder { UUID.randomUUID().toString() }

    /** 追加候補となる録画マクロ一覧。 */
    val availableRecordings: StateFlow<List<Recording>> =
        repository.observeRecordings()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 編集中のユニット。 */
    private val _unit = MutableStateFlow(
        MacroUnit(
            id = UUID.randomUUID().toString(),
            displayName = "新規ユニット",
            createdAtEpochMs = System.currentTimeMillis(),
            elements = emptyList(),
        ),
    )
    val unit: StateFlow<MacroUnit> = _unit.asStateFlow()

    fun setDisplayName(name: String) {
        _unit.value = _unit.value.copy(displayName = name)
    }

    fun setRepeatCount(value: Int) {
        _unit.value = _unit.value.copy(repeatCount = value.coerceAtLeast(0))
    }

    fun setLoopWaitMs(value: Long) {
        _unit.value = _unit.value.copy(loopWaitMs = value.coerceAtLeast(0))
    }

    fun addRecording(recording: Recording) {
        _unit.value = builder.addRecording(_unit.value, recording)
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        _unit.value = builder.moveElement(_unit.value, index, index - 1)
    }

    fun moveDown(index: Int) {
        if (index >= _unit.value.elements.lastIndex) return
        _unit.value = builder.moveElement(_unit.value, index, index + 1)
    }

    fun duplicate(index: Int) {
        _unit.value = builder.duplicateElement(_unit.value, index)
    }

    fun remove(index: Int) {
        _unit.value = builder.removeElement(_unit.value, index)
    }

    fun changeExtraWaitMs(index: Int, extraWaitMs: Long) {
        _unit.value = builder.changeExtraWait(_unit.value, index, extraWaitMs.coerceAtLeast(0))
    }

    fun save(onSaved: () -> Unit) = viewModelScope.launch {
        repository.saveUnit(_unit.value)
        onSaved()
    }
}

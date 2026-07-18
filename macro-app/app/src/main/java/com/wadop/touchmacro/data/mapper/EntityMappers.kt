package com.wadop.touchmacro.data.mapper

import com.wadop.touchmacro.core.model.MacroUnit
import com.wadop.touchmacro.core.model.Recording
import com.wadop.touchmacro.data.local.RecordingEntity
import com.wadop.touchmacro.data.local.UnitEntity
import com.wadop.touchmacro.data.serialization.OperationSerialization

/**
 * Room エンティティ ⇔ core ドメインモデルのマッピング。
 * 操作/構成要素の実データは JSON 列を介して復元する。
 */
object EntityMappers {

    fun RecordingEntity.toModel(): Recording = Recording(
        id = id,
        displayName = displayName,
        createdAtEpochMs = createdAtEpochMs,
        operations = OperationSerialization.decodeOperations(operationsJson),
        lastPlayedAtEpochMs = lastPlayedAtEpochMs,
    )

    fun Recording.toEntity(
        repeatCount: Int,
        loopWaitMs: Long,
        isDraft: Boolean,
    ): RecordingEntity = RecordingEntity(
        id = id,
        displayName = displayName,
        createdAtEpochMs = createdAtEpochMs,
        lastPlayedAtEpochMs = lastPlayedAtEpochMs,
        repeatCount = repeatCount,
        loopWaitMs = loopWaitMs,
        isDraft = isDraft,
        operationsJson = OperationSerialization.encodeOperations(operations),
    )

    fun UnitEntity.toModel(): MacroUnit = MacroUnit(
        id = id,
        displayName = displayName,
        createdAtEpochMs = createdAtEpochMs,
        elements = OperationSerialization.decodeElements(elementsJson),
        repeatCount = repeatCount,
        loopWaitMs = loopWaitMs,
        lastPlayedAtEpochMs = lastPlayedAtEpochMs,
    )

    fun MacroUnit.toEntity(): UnitEntity = UnitEntity(
        id = id,
        displayName = displayName,
        createdAtEpochMs = createdAtEpochMs,
        lastPlayedAtEpochMs = lastPlayedAtEpochMs,
        repeatCount = repeatCount,
        loopWaitMs = loopWaitMs,
        elementsJson = OperationSerialization.encodeElements(elements),
    )
}

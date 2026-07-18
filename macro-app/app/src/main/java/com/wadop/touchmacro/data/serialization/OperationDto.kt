package com.wadop.touchmacro.data.serialization

import com.wadop.touchmacro.core.model.CoordinatePayload
import com.wadop.touchmacro.core.model.Operation
import com.wadop.touchmacro.core.model.OperationPayload
import com.wadop.touchmacro.core.model.OperationStatus
import com.wadop.touchmacro.core.model.PointerTrace
import com.wadop.touchmacro.core.model.TracePoint
import com.wadop.touchmacro.core.model.UnitElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 操作データの永続化用 DTO（JSON 直列化可能）。
 *
 * core のドメインモデルは Android/直列化に非依存に保ちたいため、
 * ここで直列化可能な鏡像を定義し、Room には JSON 文字列として保存する。
 *
 * payload は sealed で多態にしており、将来 UI 要素記録方式など
 * 座標以外の操作タイプを追加してもマイグレーションしやすい。
 */
@Serializable
data class TracePointDto(val t: Long, val x: Float, val y: Float, val p: Float = 1f)

@Serializable
data class PointerTraceDto(val pointerId: Int, val points: List<TracePointDto>)

@Serializable
sealed interface OperationPayloadDto

@Serializable
@SerialName("coordinate")
data class CoordinatePayloadDto(val pointers: List<PointerTraceDto>) : OperationPayloadDto

@Serializable
data class OperationDto(
    val id: String,
    val index: Int,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val elapsedFromStartMs: Long,
    val status: String,
    val waitAfterMs: Long,
    val payload: OperationPayloadDto,
)

@Serializable
data class UnitElementDto(
    val elementId: String,
    val sourceRecordingId: String?,
    val sourceDisplayName: String,
    val operations: List<OperationDto>,
    val extraWaitAfterMs: Long,
)

/**
 * DTO ⇔ ドメインモデルの相互変換と JSON 直列化。
 */
object OperationSerialization {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "kind"
    }

    // ---- ドメイン → DTO ----

    private fun TracePoint.toDto() = TracePointDto(tOffsetMs, x, y, pressure)
    private fun PointerTrace.toDto() = PointerTraceDto(pointerId, points.map { it.toDto() })

    private fun OperationPayload.toDto(): OperationPayloadDto = when (this) {
        is CoordinatePayload -> CoordinatePayloadDto(pointers.map { it.toDto() })
    }

    fun Operation.toDto(): OperationDto = OperationDto(
        id = id,
        index = index,
        startedAtEpochMs = startedAtEpochMs,
        durationMs = durationMs,
        elapsedFromStartMs = elapsedFromStartMs,
        status = status.name,
        waitAfterMs = waitAfterMs,
        payload = payload.toDto(),
    )

    fun UnitElement.toDto(): UnitElementDto = UnitElementDto(
        elementId = elementId,
        sourceRecordingId = sourceRecordingId,
        sourceDisplayName = sourceDisplayName,
        operations = operations.map { it.toDto() },
        extraWaitAfterMs = extraWaitAfterMs,
    )

    // ---- DTO → ドメイン ----

    private fun TracePointDto.toModel() = TracePoint(t, x, y, p)
    private fun PointerTraceDto.toModel() = PointerTrace(pointerId, points.map { it.toModel() })

    private fun OperationPayloadDto.toModel(): OperationPayload = when (this) {
        is CoordinatePayloadDto -> CoordinatePayload(pointers.map { it.toModel() })
    }

    fun OperationDto.toModel(): Operation = Operation(
        id = id,
        index = index,
        startedAtEpochMs = startedAtEpochMs,
        durationMs = durationMs,
        elapsedFromStartMs = elapsedFromStartMs,
        status = OperationStatus.valueOf(status),
        waitAfterMs = waitAfterMs,
        payload = payload.toModel(),
    )

    fun UnitElementDto.toModel(): UnitElement = UnitElement(
        elementId = elementId,
        sourceRecordingId = sourceRecordingId,
        sourceDisplayName = sourceDisplayName,
        operations = operations.map { it.toModel() },
        extraWaitAfterMs = extraWaitAfterMs,
    )

    // ---- JSON 文字列 ⇔ リスト ----

    fun encodeOperations(ops: List<Operation>): String =
        json.encodeToString(ops.map { it.toDto() })

    fun decodeOperations(text: String): List<Operation> =
        json.decodeFromString<List<OperationDto>>(text).map { it.toModel() }

    fun encodeElements(elements: List<UnitElement>): String =
        json.encodeToString(elements.map { it.toDto() })

    fun decodeElements(text: String): List<UnitElement> =
        json.decodeFromString<List<UnitElementDto>>(text).map { it.toModel() }
}

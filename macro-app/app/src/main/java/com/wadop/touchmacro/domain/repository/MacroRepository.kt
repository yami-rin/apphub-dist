package com.wadop.touchmacro.domain.repository

import com.wadop.touchmacro.core.model.MacroUnit
import com.wadop.touchmacro.core.model.Operation
import com.wadop.touchmacro.core.model.Recording
import kotlinx.coroutines.flow.Flow

/**
 * マクロ資産（録画マクロ・ユニット）の永続化を抽象化するリポジトリ。
 * ドメイン層に置き、UI/サービスはこのインターフェースにのみ依存する。
 */
interface MacroRepository {

    fun observeRecordings(): Flow<List<Recording>>
    fun observeUnits(): Flow<List<MacroUnit>>

    suspend fun getRecording(id: String): Recording?
    suspend fun getUnit(id: String): MacroUnit?

    /**
     * 録画を確定保存する。初期名称 "YYYYMMDD-通番" は採番して付与する。
     * @param isDraft 一時保存（復旧用）かどうか
     */
    suspend fun saveRecording(
        operations: List<Operation>,
        createdAtEpochMs: Long,
        dateKey: String,
        isDraft: Boolean = false,
    ): Recording

    /** 一時保存(draft)を確定保存へ昇格する。 */
    suspend fun promoteDraft(id: String)

    /** 復旧可能な一時保存の一覧。 */
    suspend fun getDrafts(): List<Recording>

    suspend fun renameRecording(id: String, name: String)
    suspend fun duplicateRecording(id: String, newId: String, newName: String, now: Long)
    suspend fun deleteRecording(id: String)
    suspend fun updateRecordingPlaybackSettings(id: String, repeatCount: Int, loopWaitMs: Long)
    suspend fun updateRecordingLastPlayed(id: String, ts: Long)

    /** 録画の再生設定（再生回数, ループ間待機ms）を取得する。未登録なら (1, 0)。 */
    suspend fun getRecordingPlaybackSettings(id: String): Pair<Int, Long>

    suspend fun saveUnit(unit: MacroUnit)
    suspend fun renameUnit(id: String, name: String)
    suspend fun deleteUnit(id: String)
    suspend fun updateUnitLastPlayed(id: String, ts: Long)
}

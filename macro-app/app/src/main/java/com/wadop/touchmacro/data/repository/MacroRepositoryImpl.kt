package com.wadop.touchmacro.data.repository

import com.wadop.touchmacro.core.model.MacroUnit
import com.wadop.touchmacro.core.model.Operation
import com.wadop.touchmacro.core.model.Recording
import com.wadop.touchmacro.core.util.RecordingNaming
import com.wadop.touchmacro.data.local.RecordingDao
import com.wadop.touchmacro.data.local.SequenceCounterEntity
import com.wadop.touchmacro.data.local.SequenceDao
import com.wadop.touchmacro.data.local.UnitDao
import com.wadop.touchmacro.data.mapper.EntityMappers.toEntity
import com.wadop.touchmacro.data.mapper.EntityMappers.toModel
import com.wadop.touchmacro.domain.repository.MacroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MacroRepository] の Room 実装。
 */
@Singleton
class MacroRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao,
    private val unitDao: UnitDao,
    private val sequenceDao: SequenceDao,
) : MacroRepository {

    override fun observeRecordings(): Flow<List<Recording>> =
        recordingDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeUnits(): Flow<List<MacroUnit>> =
        unitDao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun getRecording(id: String): Recording? =
        recordingDao.getById(id)?.toModel()

    override suspend fun getUnit(id: String): MacroUnit? =
        unitDao.getById(id)?.toModel()

    override suspend fun saveRecording(
        operations: List<Operation>,
        createdAtEpochMs: Long,
        dateKey: String,
        isDraft: Boolean,
    ): Recording {
        // 通番採番（削除番号は再利用しない）
        val lastIssued = sequenceDao.getLastIssued(dateKey) ?: 0
        val naming = RecordingNaming.nextName(dateKey, lastIssued)
        sequenceDao.put(SequenceCounterEntity(dateKey, naming.issuedSeq))

        val recording = Recording(
            id = UUID.randomUUID().toString(),
            displayName = naming.name,
            createdAtEpochMs = createdAtEpochMs,
            operations = operations,
            lastPlayedAtEpochMs = null,
        )
        recordingDao.upsert(
            recording.toEntity(repeatCount = 1, loopWaitMs = 0, isDraft = isDraft),
        )
        return recording
    }

    override suspend fun promoteDraft(id: String) {
        val entity = recordingDao.getById(id) ?: return
        recordingDao.update(entity.copy(isDraft = false))
    }

    override suspend fun getDrafts(): List<Recording> =
        recordingDao.getDrafts().map { it.toModel() }

    override suspend fun renameRecording(id: String, name: String) =
        recordingDao.rename(id, name)

    override suspend fun duplicateRecording(id: String, newId: String, newName: String, now: Long) {
        val src = recordingDao.getById(id) ?: return
        recordingDao.upsert(
            src.copy(
                id = newId,
                displayName = newName,
                createdAtEpochMs = now,
                lastPlayedAtEpochMs = null,
                isDraft = false,
            ),
        )
    }

    override suspend fun deleteRecording(id: String) = recordingDao.delete(id)

    override suspend fun updateRecordingPlaybackSettings(id: String, repeatCount: Int, loopWaitMs: Long) =
        recordingDao.updatePlaybackSettings(id, repeatCount, loopWaitMs)

    override suspend fun updateRecordingLastPlayed(id: String, ts: Long) =
        recordingDao.updateLastPlayed(id, ts)

    override suspend fun saveUnit(unit: MacroUnit) = unitDao.upsert(unit.toEntity())

    override suspend fun renameUnit(id: String, name: String) = unitDao.rename(id, name)

    override suspend fun deleteUnit(id: String) = unitDao.delete(id)

    override suspend fun updateUnitLastPlayed(id: String, ts: Long) =
        unitDao.updateLastPlayed(id, ts)

    /** 録画の再生設定を読み出す（再生プログラム生成に使用）。 */
    override suspend fun getRecordingPlaybackSettings(id: String): Pair<Int, Long> {
        val e = recordingDao.getById(id) ?: return 1 to 0L
        return e.repeatCount to e.loopWaitMs
    }
}

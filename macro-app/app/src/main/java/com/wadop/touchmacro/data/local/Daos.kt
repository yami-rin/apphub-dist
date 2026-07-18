package com.wadop.touchmacro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE isDraft = 0 ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE isDraft = 1 ORDER BY createdAtEpochMs DESC")
    suspend fun getDrafts(): List<RecordingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecordingEntity)

    @Update
    suspend fun update(entity: RecordingEntity)

    @Query("UPDATE recordings SET displayName = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE recordings SET lastPlayedAtEpochMs = :ts WHERE id = :id")
    suspend fun updateLastPlayed(id: String, ts: Long)

    @Query("UPDATE recordings SET repeatCount = :repeat, loopWaitMs = :loopWait WHERE id = :id")
    suspend fun updatePlaybackSettings(id: String, repeat: Int, loopWait: Long)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface UnitDao {
    @Query("SELECT * FROM units ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getById(id: String): UnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UnitEntity)

    @Query("UPDATE units SET displayName = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE units SET lastPlayedAtEpochMs = :ts WHERE id = :id")
    suspend fun updateLastPlayed(id: String, ts: Long)

    @Query("DELETE FROM units WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SequenceDao {
    @Query("SELECT lastIssuedSeq FROM sequence_counters WHERE dateKey = :dateKey")
    suspend fun getLastIssued(dateKey: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: SequenceCounterEntity)
}

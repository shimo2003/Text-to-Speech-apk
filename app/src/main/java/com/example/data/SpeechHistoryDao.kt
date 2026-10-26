package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeechHistoryDao {

    @Query("SELECT * FROM speech_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SpeechHistoryEntity>>

    @Query("SELECT * FROM speech_history WHERE type = :type ORDER BY timestamp DESC")
    fun getHistoryByType(type: String): Flow<List<SpeechHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SpeechHistoryEntity): Long

    @Delete
    suspend fun delete(item: SpeechHistoryEntity)

    @Query("DELETE FROM speech_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM speech_history")
    suspend fun clearAll()
}

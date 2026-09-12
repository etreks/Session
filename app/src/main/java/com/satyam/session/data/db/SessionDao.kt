package com.satyam.session.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.satyam.session.data.model.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insertSession(session: Session): Long

    @Query("SELECT * FROM sessions WHERE startTime >= :startMs AND startTime < :endMs ORDER BY startTime ASC")
    fun getSessionsInRange(startMs: Long, endMs: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Delete
    suspend fun deleteSession(session: Session)
}

package com.satyam.session.data.repository

import com.satyam.session.data.db.ActivityBucketDao
import com.satyam.session.data.db.SessionDao
import com.satyam.session.data.model.ActivityBucket
import com.satyam.session.data.model.Session
import kotlinx.coroutines.flow.Flow

class SessionRepository(
    private val bucketDao: ActivityBucketDao,
    private val sessionDao: SessionDao
) {
    val allBuckets: Flow<List<ActivityBucket>> = bucketDao.getAllBuckets()

    suspend fun insertBucket(bucket: ActivityBucket): Long = bucketDao.insertBucket(bucket)

    suspend fun deleteBucket(bucket: ActivityBucket) = bucketDao.deleteBucket(bucket)

    suspend fun getBucketCount(): Int = bucketDao.getCount()

    suspend fun insertSession(session: Session): Long = sessionDao.insertSession(session)

    fun getSessionsInRange(startMs: Long, endMs: Long): Flow<List<Session>> =
        sessionDao.getSessionsInRange(startMs, endMs)

    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAllSessions()

    suspend fun deleteSession(session: Session) = sessionDao.deleteSession(session)
}

package com.satyam.session.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.satyam.session.data.model.ActivityBucket
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityBucketDao {

    @Query("SELECT * FROM activity_buckets ORDER BY createdAt ASC")
    fun getAllBuckets(): Flow<List<ActivityBucket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBucket(bucket: ActivityBucket): Long

    @Delete
    suspend fun deleteBucket(bucket: ActivityBucket)

    @Query("SELECT COUNT(*) FROM activity_buckets")
    suspend fun getCount(): Int
}

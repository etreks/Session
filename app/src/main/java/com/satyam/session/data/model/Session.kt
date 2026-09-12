package com.satyam.session.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = ActivityBucket::class,
            parentColumns = ["id"],
            childColumns = ["bucketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bucketId"])]
)
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bucketId: Long,
    val activityLabel: String = "",
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val wasAutoStopped: Boolean = false
)

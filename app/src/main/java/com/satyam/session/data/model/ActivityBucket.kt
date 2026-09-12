package com.satyam.session.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_buckets")
data class ActivityBucket(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis()
)

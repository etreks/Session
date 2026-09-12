package com.satyam.session.util

import com.satyam.session.data.model.ActivityBucket

/** Default buckets seeded on first launch. */
object PresetBuckets {
    val defaults = listOf(
        ActivityBucket(name = "Study", colorHex = "#4CAF50"),
        ActivityBucket(name = "Exercise", colorHex = "#2196F3"),
        ActivityBucket(name = "Reading", colorHex = "#FFC107"),
        ActivityBucket(name = "Work", colorHex = "#FF9800"),
    )
}

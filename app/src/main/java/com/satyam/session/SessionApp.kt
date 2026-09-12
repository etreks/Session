package com.satyam.session

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.satyam.session.data.db.SessionDatabase
import com.satyam.session.data.repository.SessionRepository
import com.satyam.session.util.PresetBuckets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SessionApp : Application() {

    val database by lazy { SessionDatabase.getDatabase(this) }
    val repository by lazy {
        SessionRepository(database.activityBucketDao(), database.sessionDao())
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        seedDefaultBuckets()
    }

    private fun createNotificationChannels() {
        val stopwatchChannel = NotificationChannel(
            STOPWATCH_CHANNEL_ID,
            "Stopwatch",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing stopwatch notification"
        }

        val warningChannel = NotificationChannel(
            WARNING_CHANNEL_ID,
            "Session Warnings",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Warnings when session is about to auto-stop"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(stopwatchChannel)
        manager.createNotificationChannel(warningChannel)
    }

    /** Seed Study/Exercise/Reading/Work buckets on first ever launch. */
    private fun seedDefaultBuckets() {
        applicationScope.launch {
            if (repository.getBucketCount() == 0) {
                PresetBuckets.defaults.forEach { bucket ->
                    repository.insertBucket(bucket)
                }
            }
        }
    }

    companion object {
        const val STOPWATCH_CHANNEL_ID = "stopwatch_channel"
        const val WARNING_CHANNEL_ID = "warning_channel"
    }
}

package com.satyam.session.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.satyam.session.MainActivity
import com.satyam.session.SessionApp
import com.satyam.session.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Data passed from service → ViewModel when a session auto-stops. */
data class SessionSaveData(
    val startTime: Long,
    val endTime: Long,
    val wasAutoStopped: Boolean
)

/**
 * Foreground service that keeps the stopwatch running when the app is backgrounded.
 *
 * Auto-stop logic (only when screen is ON and app is in background):
 *   - 2 min away → warning notification
 *   - 4 min away → urgent notification
 *   - 5 min away → auto-stop, session saved silently
 *   - Screen off → trusted, no limit
 */
class StopwatchService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var awayTimerJob: Job? = null
    private var lifecycleObserver: LifecycleEventObserver? = null

    companion object {
        val isRunning = MutableStateFlow(false)
        val elapsedMs = MutableStateFlow(0L)
        val sessionToSave = MutableStateFlow<SessionSaveData?>(null)
        var startTimeMs: Long = 0L
            private set

        private const val NOTIFICATION_ID = 1
        private const val WARNING_NOTIFICATION_ID = 2
        private const val AWAY_WARNING_2MIN_MS = 2 * 60 * 1000L
        private const val AWAY_WARNING_4MIN_MS = 4 * 60 * 1000L
        private const val AWAY_AUTO_STOP_MS = 5 * 60 * 1000L

        fun startTimer(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply {
                action = "START"
            }
            context.startForegroundService(intent)
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply {
                action = "STOP"
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startStopwatch()
            "STOP" -> stopStopwatch()
        }
        return START_STICKY
    }

    private fun startStopwatch() {
        startTimeMs = System.currentTimeMillis()
        isRunning.value = true
        elapsedMs.value = 0L

        startForeground(NOTIFICATION_ID, createNotification(0L))
        launchTimerTick()
        observeLifecycle()
    }

    private fun stopStopwatch() {
        timerJob?.cancel()
        awayTimerJob?.cancel()
        isRunning.value = false

        lifecycleObserver?.let {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(it)
        }
        cancelWarningNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Timer tick ───────────────────────────────────────────────────────

    private fun launchTimerTick() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                elapsedMs.value = System.currentTimeMillis() - startTimeMs
                updateNotification(elapsedMs.value)
                delay(200) // 5 fps for notification, UI polls at higher rate
            }
        }
    }

    // ── Away detection ───────────────────────────────────────────────────

    private fun observeLifecycle() {
        lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // App went to background — start away countdown only if screen is on
                    if (isRunning.value && isScreenOn()) {
                        startAwayTimer()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    // App came back to foreground — cancel any away countdown
                    cancelAwayTimer()
                    cancelWarningNotification()
                }
                else -> { /* no-op */ }
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver!!)
    }

    private fun isScreenOn(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isInteractive
    }

    private fun startAwayTimer() {
        awayTimerJob?.cancel()
        awayTimerJob = serviceScope.launch {
            var awayMs = 0L
            val tickMs = 1_000L
            var warned2 = false
            var warned4 = false

            while (awayMs < AWAY_AUTO_STOP_MS && isActive) {
                delay(tickMs)
                // If user turned screen off during away period → trust them, cancel countdown
                if (!isScreenOn()) return@launch
                awayMs += tickMs

                if (awayMs >= AWAY_WARNING_2MIN_MS && !warned2) {
                    warned2 = true
                    showWarningNotification("Come back! Session stopping in 3 minutes")
                }
                if (awayMs >= AWAY_WARNING_4MIN_MS && !warned4) {
                    warned4 = true
                    showWarningNotification("⚠️ Last minute! Session auto-stopping in 60s")
                }
            }

            // 5 minutes elapsed with screen on — auto-stop and save silently
            val endTime = System.currentTimeMillis()
            sessionToSave.value = SessionSaveData(
                startTime = startTimeMs,
                endTime = endTime,
                wasAutoStopped = true
            )
            stopStopwatch()
        }
    }

    private fun cancelAwayTimer() {
        awayTimerJob?.cancel()
        awayTimerJob = null
    }

    // ── Notifications ────────────────────────────────────────────────────

    private fun createNotification(elapsed: Long): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, SessionApp.STOPWATCH_CHANNEL_ID)
            .setContentTitle("Session running")
            .setContentText(TimeUtils.formatDuration(elapsed))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(elapsed: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(elapsed))
    }

    private fun showWarningNotification(message: String) {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, SessionApp.WARNING_CHANNEL_ID)
            .setContentTitle("Session Warning")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(WARNING_NOTIFICATION_ID, notification)
    }

    private fun cancelWarningNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(WARNING_NOTIFICATION_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        lifecycleObserver?.let {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(it)
        }
    }
}

package com.satyam.session.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
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

/** Data passed from service → ViewModel when a session finishes or auto-stops. */
data class SessionSaveData(
    val startTime: Long,
    val endTime: Long,
    val wasAutoStopped: Boolean
)

/**
 * Foreground service that powers both the open-ended Stopwatch and the Focus Countdown Timer.
 */
class StopwatchService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var awayTimerJob: Job? = null
    private var lifecycleObserver: LifecycleEventObserver? = null

    companion object {
        val isRunning = MutableStateFlow(false)
        val isPaused = MutableStateFlow(false)
        val isTimerMode = MutableStateFlow(false)
        val targetDurationMs = MutableStateFlow(0L)
        val elapsedMs = MutableStateFlow(0L)
        val remainingMs = MutableStateFlow(0L)
        val sessionToSave = MutableStateFlow<SessionSaveData?>(null)

        var startTimeMs: Long = 0L
            private set
        private var pausedAtMs: Long = 0L
        private var totalPausedDurationMs: Long = 0L

        private const val NOTIFICATION_ID = 1
        private const val WARNING_NOTIFICATION_ID = 2
        private const val ALARM_NOTIFICATION_ID = 3
        private const val AWAY_WARNING_2MIN_MS = 2 * 60 * 1000L
        private const val AWAY_WARNING_4MIN_MS = 4 * 60 * 1000L
        private const val AWAY_AUTO_STOP_MS = 5 * 60 * 1000L

        fun startStopwatch(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply {
                action = "START_STOPWATCH"
            }
            context.startForegroundService(intent)
        }

        fun startTimer(context: Context, durationMs: Long) {
            val intent = Intent(context, StopwatchService::class.java).apply {
                action = "START_TIMER"
                putExtra("TARGET_DURATION_MS", durationMs)
            }
            context.startForegroundService(intent)
        }

        fun pauseTimer(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply {
                action = "PAUSE_TIMER"
            }
            context.startService(intent)
        }

        fun resumeTimer(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply {
                action = "RESUME_TIMER"
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply {
                action = "STOP"
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_STOPWATCH" -> startStopwatchMode()
            "START_TIMER" -> {
                val duration = intent.getLongExtra("TARGET_DURATION_MS", 25 * 60 * 1000L)
                startTimerMode(duration)
            }
            "PAUSE_TIMER" -> pause()
            "RESUME_TIMER" -> resume()
            "STOP" -> stopAll()
        }
        return START_STICKY
    }

    private fun startStopwatchMode() {
        isTimerMode.value = false
        targetDurationMs.value = 0L
        startTimeMs = System.currentTimeMillis()
        totalPausedDurationMs = 0L
        isPaused.value = false
        isRunning.value = true
        elapsedMs.value = 0L
        remainingMs.value = 0L

        startForeground(NOTIFICATION_ID, createNotification("Session running", "00:00:00"))
        launchStopwatchTick()
        observeLifecycle()
    }

    private fun startTimerMode(duration: Long) {
        isTimerMode.value = true
        targetDurationMs.value = duration
        startTimeMs = System.currentTimeMillis()
        totalPausedDurationMs = 0L
        isPaused.value = false
        isRunning.value = true
        elapsedMs.value = 0L
        remainingMs.value = duration

        startForeground(NOTIFICATION_ID, createNotification("Focus Timer", TimeUtils.formatDuration(duration)))
        launchTimerCountdown(duration)
        observeLifecycle()
    }

    private fun pause() {
        if (!isRunning.value || isPaused.value) return
        isPaused.value = true
        pausedAtMs = System.currentTimeMillis()
        timerJob?.cancel()
        updateNotification(if (isTimerMode.value) "Focus Timer (Paused)" else "Stopwatch (Paused)", TimeUtils.formatDuration(remainingMs.value))
    }

    private fun resume() {
        if (!isRunning.value || !isPaused.value) return
        isPaused.value = false
        totalPausedDurationMs += (System.currentTimeMillis() - pausedAtMs)
        if (isTimerMode.value) {
            launchTimerCountdown(targetDurationMs.value)
        } else {
            launchStopwatchTick()
        }
    }

    private fun stopAll() {
        timerJob?.cancel()
        awayTimerJob?.cancel()
        isRunning.value = false
        isPaused.value = false

        lifecycleObserver?.let {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(it)
        }
        cancelWarningNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Stopwatch Ticking ────────────────────────────────────────────────

    private fun launchStopwatchTick() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && !isPaused.value) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTimeMs - totalPausedDurationMs
                elapsedMs.value = elapsed.coerceAtLeast(0L)
                updateNotification("Session running", TimeUtils.formatDuration(elapsedMs.value))
                delay(200)
            }
        }
    }

    // ── Countdown Timer Ticking ──────────────────────────────────────────

    private fun launchTimerCountdown(totalDuration: Long) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && !isPaused.value) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTimeMs - totalPausedDurationMs
                val remaining = totalDuration - elapsed

                if (remaining <= 0) {
                    // Timer finished!
                    elapsedMs.value = totalDuration
                    remainingMs.value = 0L
                    onTimerFinished(totalDuration)
                    break
                } else {
                    elapsedMs.value = elapsed.coerceAtLeast(0L)
                    remainingMs.value = remaining
                    updateNotification("Focus Timer", "${TimeUtils.formatDuration(remaining)} remaining")
                }
                delay(200)
            }
        }
    }

    private fun onTimerFinished(totalDuration: Long) {
        val endTime = System.currentTimeMillis()
        sessionToSave.value = SessionSaveData(
            startTime = startTimeMs,
            endTime = endTime,
            wasAutoStopped = false
        )
        showTimerCompletionAlarm()
        stopAll()
    }

    private fun showTimerCompletionAlarm() {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, SessionApp.TIMER_ALARM_CHANNEL_ID)
            .setContentTitle("🎉 Focus Session Completed!")
            .setContentText("Great job! Your focus time has been recorded to your history.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(ALARM_NOTIFICATION_ID, notification)
    }

    // ── Away Detection (Screen-On Anti-Distraction) ───────────────────────

    private fun observeLifecycle() {
        lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (isRunning.value && !isPaused.value && isScreenOn()) {
                        startAwayTimer()
                    }
                }
                Lifecycle.Event.ON_START -> {
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
                if (!isScreenOn()) return@launch
                awayMs += tickMs

                if (awayMs >= AWAY_WARNING_2MIN_MS && !warned2) {
                    warned2 = true
                    showWarningNotification("Come back! Session pausing in 3 minutes")
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
            stopAll()
        }
    }

    private fun cancelAwayTimer() {
        awayTimerJob?.cancel()
        awayTimerJob = null
    }

    // ── Notifications ────────────────────────────────────────────────────

    private fun createNotification(title: String, subtitle: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, SessionApp.STOPWATCH_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(title: String, subtitle: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(title, subtitle))
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

package com.satyam.session.ui.stopwatch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.satyam.session.SessionApp
import com.satyam.session.data.model.ActivityBucket
import com.satyam.session.data.model.Session
import com.satyam.session.service.StopwatchService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopwatchUiState(
    val isTimerMode: Boolean = false, // false = Stopwatch, true = Focus Countdown Timer
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    val remainingMs: Long = 0L,
    val targetDurationMs: Long = 25 * 60 * 1000L, // Default 25m Pomodoro
    val timerDigits: String = "2500", // Default display "25:00"
    val selectedPresetMinutes: Int? = 25,
    val selectedBucket: ActivityBucket? = null,
    val activityLabel: String = "",
    val buckets: List<ActivityBucket> = emptyList(),
    val showCreateBucket: Boolean = false,
    val sessionJustSaved: Boolean = false,
)

class StopwatchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SessionApp
    private val repository = app.repository

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    init {
        // Observe buckets from DB
        viewModelScope.launch {
            repository.allBuckets.collect { buckets ->
                _uiState.update { state ->
                    state.copy(
                        buckets = buckets,
                        selectedBucket = state.selectedBucket ?: buckets.firstOrNull()
                    )
                }
            }
        }

        // Poll elapsed & remaining time from the service (~20 fps for smooth UI)
        viewModelScope.launch {
            while (true) {
                val running = StopwatchService.isRunning.value
                val paused = StopwatchService.isPaused.value
                val isServiceTimerMode = StopwatchService.isTimerMode.value

                if (running) {
                    _uiState.update {
                        it.copy(
                            isRunning = true,
                            isPaused = paused,
                            isTimerMode = isServiceTimerMode,
                            elapsedMs = StopwatchService.elapsedMs.value,
                            remainingMs = StopwatchService.remainingMs.value,
                            targetDurationMs = StopwatchService.targetDurationMs.value
                        )
                    }
                }
                delay(50)
            }
        }

        // Watch for service stopping
        viewModelScope.launch {
            StopwatchService.isRunning.collect { running ->
                if (!running && _uiState.value.isRunning) {
                    _uiState.update { it.copy(isRunning = false, isPaused = false) }
                }
            }
        }

        // Watch for session saves from the service (e.g. timer finished or auto-stop)
        viewModelScope.launch {
            StopwatchService.sessionToSave.collect { data ->
                if (data != null) {
                    saveSession(data.startTime, data.endTime, data.wasAutoStopped)
                    StopwatchService.sessionToSave.value = null
                }
            }
        }
    }

    fun setMode(isTimer: Boolean) {
        if (!_uiState.value.isRunning) {
            _uiState.update { it.copy(isTimerMode = isTimer) }
        }
    }

    fun selectBucket(bucket: ActivityBucket) {
        _uiState.update { it.copy(selectedBucket = bucket) }
    }

    fun updateLabel(label: String) {
        _uiState.update { it.copy(activityLabel = label) }
    }

    // ── Timer Input Actions ──────────────────────────────────────────────

    fun inputDigit(char: Char) {
        val current = _uiState.value.timerDigits
        if (current.length >= 6) return // Max HHMMSS (6 digits)
        val newDigits = if (current == "0") char.toString() else current + char
        updateTimerFromDigits(newDigits, null)
    }

    fun inputDoubleZero() {
        val current = _uiState.value.timerDigits
        if (current.isEmpty() || current == "0") return
        if (current.length > 4) return
        val newDigits = current + "00"
        updateTimerFromDigits(newDigits, null)
    }

    fun backspaceDigit() {
        val current = _uiState.value.timerDigits
        if (current.isNotEmpty()) {
            val newDigits = current.dropLast(1)
            updateTimerFromDigits(newDigits, null)
        }
    }

    fun setPresetDuration(minutes: Int) {
        val digits = String.format("%02d00", minutes)
        val durationMs = minutes * 60 * 1000L
        _uiState.update {
            it.copy(
                timerDigits = digits,
                selectedPresetMinutes = minutes,
                targetDurationMs = durationMs,
                remainingMs = durationMs
            )
        }
    }

    private fun updateTimerFromDigits(digits: String, preset: Int?) {
        val padded = digits.padStart(6, '0').takeLast(6)
        val hours = padded.substring(0, 2).toIntOrNull() ?: 0
        val minutes = padded.substring(2, 4).toIntOrNull() ?: 0
        val seconds = padded.substring(4, 6).toIntOrNull() ?: 0
        val totalMs = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L

        _uiState.update {
            it.copy(
                timerDigits = digits,
                selectedPresetMinutes = preset,
                targetDurationMs = totalMs,
                remainingMs = totalMs
            )
        }
    }

    // ── Timer Start / Pause / Stop ───────────────────────────────────────

    fun startTimer() {
        val duration = _uiState.value.targetDurationMs
        if (duration <= 0 || _uiState.value.selectedBucket == null) return
        StopwatchService.startTimer(getApplication(), duration)
        _uiState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                remainingMs = duration,
                sessionJustSaved = false
            )
        }
    }

    fun pauseTimer() {
        StopwatchService.pauseTimer(getApplication())
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeTimer() {
        StopwatchService.resumeTimer(getApplication())
        _uiState.update { it.copy(isPaused = false) }
    }

    fun cancelOrStopTimer() {
        val elapsed = StopwatchService.elapsedMs.value
        val startTime = StopwatchService.startTimeMs
        StopwatchService.stop(getApplication())

        // Save whatever focused time was completed (if >= 10s)
        if (elapsed >= 10_000L) {
            saveSession(startTime, startTime + elapsed, wasAutoStopped = false)
        }
        _uiState.update { it.copy(isRunning = false, isPaused = false, elapsedMs = 0L) }
    }

    // ── Stopwatch Actions ────────────────────────────────────────────────

    fun startStopwatch() {
        if (_uiState.value.selectedBucket == null) return
        StopwatchService.startStopwatch(getApplication())
        _uiState.update { it.copy(isRunning = true, elapsedMs = 0L, sessionJustSaved = false) }
    }

    fun stopStopwatch() {
        val elapsed = StopwatchService.elapsedMs.value
        val startTime = StopwatchService.startTimeMs
        StopwatchService.stop(getApplication())
        saveSession(startTime, startTime + elapsed, wasAutoStopped = false)
        _uiState.update { it.copy(isRunning = false, elapsedMs = 0L) }
    }

    private fun saveSession(startTime: Long, endTime: Long, wasAutoStopped: Boolean) {
        val state = _uiState.value
        val bucket = state.selectedBucket ?: return
        val durationMs = endTime - startTime
        if (durationMs < 1_000) return // ignore sub-second sessions

        viewModelScope.launch {
            repository.insertSession(
                Session(
                    bucketId = bucket.id,
                    activityLabel = state.activityLabel,
                    startTime = startTime,
                    endTime = endTime,
                    durationMs = durationMs,
                    wasAutoStopped = wasAutoStopped
                )
            )
            _uiState.update {
                it.copy(activityLabel = "", sessionJustSaved = true, elapsedMs = 0L)
            }
        }
    }

    fun showCreateBucketDialog() = _uiState.update { it.copy(showCreateBucket = true) }
    fun hideCreateBucketDialog() = _uiState.update { it.copy(showCreateBucket = false) }
    fun dismissSavedMessage() = _uiState.update { it.copy(sessionJustSaved = false) }

    fun createBucket(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertBucket(ActivityBucket(name = name, colorHex = colorHex))
        }
    }
}

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
    val isRunning: Boolean = false,
    val elapsedMs: Long = 0L,
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

        // Poll elapsed time from the service (~20 fps for smooth UI)
        viewModelScope.launch {
            while (true) {
                val running = StopwatchService.isRunning.value
                if (running) {
                    _uiState.update {
                        it.copy(isRunning = true, elapsedMs = StopwatchService.elapsedMs.value)
                    }
                }
                delay(50)
            }
        }

        // Watch for service stopping (e.g. auto-stop)
        viewModelScope.launch {
            StopwatchService.isRunning.collect { running ->
                if (!running && _uiState.value.isRunning) {
                    _uiState.update { it.copy(isRunning = false) }
                }
            }
        }

        // Watch for auto-stop session saves from the service
        viewModelScope.launch {
            StopwatchService.sessionToSave.collect { data ->
                if (data != null) {
                    saveSession(data.startTime, data.endTime, data.wasAutoStopped)
                    StopwatchService.sessionToSave.value = null
                }
            }
        }
    }

    fun selectBucket(bucket: ActivityBucket) {
        _uiState.update { it.copy(selectedBucket = bucket) }
    }

    fun updateLabel(label: String) {
        _uiState.update { it.copy(activityLabel = label) }
    }

    fun startStopwatch() {
        if (_uiState.value.selectedBucket == null) return
        StopwatchService.startTimer(getApplication())
        _uiState.update { it.copy(isRunning = true, elapsedMs = 0L, sessionJustSaved = false) }
    }

    fun stopStopwatch() {
        val elapsed = StopwatchService.elapsedMs.value
        val startTime = StopwatchService.startTimeMs
        StopwatchService.stopTimer(getApplication())
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

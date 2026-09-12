package com.satyam.session.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.satyam.session.SessionApp
import com.satyam.session.data.model.ActivityBucket
import com.satyam.session.data.model.Session
import com.satyam.session.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BucketStats(
    val bucket: ActivityBucket,
    val totalDurationMs: Long,
    val sessionCount: Int,
    val percentage: Float,
    val sessions: List<Session>,
)

data class StatsUiState(
    val isWeekView: Boolean = false,
    val bucketStats: List<BucketStats> = emptyList(),
    val totalDurationMs: Long = 0L,
    val totalSessionCount: Int = 0,
    val averageDurationMs: Long = 0L,
    val topBucketName: String? = null,
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SessionApp
    private val repository = app.repository

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _isWeekView = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(
                repository.allBuckets,
                _isWeekView
            ) { buckets, isWeek -> Pair(buckets, isWeek) }
                .collectLatest { (buckets, isWeek) ->
                    val startMs = if (isWeek) TimeUtils.getStartOfWeek() else TimeUtils.getStartOfDay()
                    val endMs = if (isWeek) TimeUtils.getEndOfWeek() else TimeUtils.getEndOfDay()

                    repository.getSessionsInRange(startMs, endMs).collect { sessions ->
                        val bucketsMap = buckets.associateBy { it.id }
                        val totalDuration = sessions.sumOf { it.durationMs }

                        val statsByBucket = sessions
                            .groupBy { it.bucketId }
                            .map { (bucketId, bucketSessions) ->
                                val bucket = bucketsMap[bucketId]
                                    ?: ActivityBucket(name = "Activity", colorHex = "#4CAF50")
                                val bucketTotal = bucketSessions.sumOf { it.durationMs }
                                val percentage = if (totalDuration > 0) {
                                    (bucketTotal.toFloat() / totalDuration.toFloat()) * 100f
                                } else 0f

                                BucketStats(
                                    bucket = bucket,
                                    totalDurationMs = bucketTotal,
                                    sessionCount = bucketSessions.size,
                                    percentage = percentage,
                                    sessions = bucketSessions.sortedByDescending { it.startTime }
                                )
                            }
                            .sortedByDescending { it.totalDurationMs }

                        val avgDuration = if (sessions.isNotEmpty()) totalDuration / sessions.size else 0L
                        val topBucket = statsByBucket.firstOrNull()?.bucket?.name

                        _uiState.update {
                            StatsUiState(
                                isWeekView = isWeek,
                                bucketStats = statsByBucket,
                                totalDurationMs = totalDuration,
                                totalSessionCount = sessions.size,
                                averageDurationMs = avgDuration,
                                topBucketName = topBucket
                            )
                        }
                    }
                }
        }
    }

    fun toggleView() {
        _isWeekView.value = !_isWeekView.value
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }
}

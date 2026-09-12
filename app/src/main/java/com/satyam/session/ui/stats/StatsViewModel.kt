package com.satyam.session.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.satyam.session.SessionApp
import com.satyam.session.data.model.ActivityBucket
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
)

data class StatsUiState(
    val isWeekView: Boolean = false,
    val bucketStats: List<BucketStats> = emptyList(),
    val totalDurationMs: Long = 0L,
    val totalSessionCount: Int = 0,
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
                        val statsByBucket = sessions
                            .groupBy { it.bucketId }
                            .map { (bucketId, bucketSessions) ->
                                BucketStats(
                                    bucket = bucketsMap[bucketId]
                                        ?: ActivityBucket(name = "Unknown", colorHex = "#888888"),
                                    totalDurationMs = bucketSessions.sumOf { it.durationMs },
                                    sessionCount = bucketSessions.size
                                )
                            }
                            .sortedByDescending { it.totalDurationMs }

                        _uiState.update {
                            StatsUiState(
                                isWeekView = isWeek,
                                bucketStats = statsByBucket,
                                totalDurationMs = sessions.sumOf { it.durationMs },
                                totalSessionCount = sessions.size
                            )
                        }
                    }
                }
        }
    }

    fun toggleView() {
        _isWeekView.value = !_isWeekView.value
    }
}

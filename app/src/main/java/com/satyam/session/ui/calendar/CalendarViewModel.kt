package com.satyam.session.ui.calendar

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val isWeekView: Boolean = false,
    val sessionsForDay: List<Session> = emptyList(),
    val sessionsForWeek: List<Session> = emptyList(),
    val buckets: Map<Long, ActivityBucket> = emptyMap(),
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SessionApp
    private val repository = app.repository

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    init {
        // Buckets as lookup map
        viewModelScope.launch {
            repository.allBuckets.collect { buckets ->
                _uiState.update { it.copy(buckets = buckets.associateBy { b -> b.id }) }
            }
        }

        // React to date changes → reload sessions
        viewModelScope.launch {
            _selectedDate.collectLatest { date ->
                _uiState.update { it.copy(selectedDate = date) }

                // Day sessions
                launch {
                    repository.getSessionsInRange(
                        TimeUtils.getStartOfDay(date),
                        TimeUtils.getEndOfDay(date)
                    ).collect { sessions ->
                        _uiState.update { it.copy(sessionsForDay = sessions) }
                    }
                }

                // Week sessions (for week-view)
                launch {
                    repository.getSessionsInRange(
                        TimeUtils.getStartOfWeek(date),
                        TimeUtils.getEndOfWeek(date)
                    ).collect { sessions ->
                        _uiState.update { it.copy(sessionsForWeek = sessions) }
                    }
                }
            }
        }
    }

    fun navigateDay(forward: Boolean) {
        _selectedDate.value = if (forward) _selectedDate.value.plusDays(1)
        else _selectedDate.value.minusDays(1)
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun toggleWeekView() {
        _uiState.update { it.copy(isWeekView = !it.isWeekView) }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _uiState.update { it.copy(isWeekView = false) }
    }
}

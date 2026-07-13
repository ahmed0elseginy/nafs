package com.nafs.app.presentation.habitdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nafs.app.domain.model.DayCell
import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.model.HabitCheckIn
import com.nafs.app.domain.model.LifeArea
import com.nafs.app.domain.model.StreakInfo
import com.nafs.app.domain.model.Trend
import com.nafs.app.domain.model.WeekPoint
import com.nafs.app.domain.repository.CheckInRepository
import com.nafs.app.domain.repository.HabitRepository
import com.nafs.app.domain.repository.LifeAreaRepository
import com.nafs.app.domain.scoring.ScoreEngine
import com.nafs.app.presentation.navigation.HabitDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HabitDetailUiState {
    data object Loading : HabitDetailUiState

    /** Emitted after deletion or for a stale link; the screen simply shows nothing. */
    data object NotFound : HabitDetailUiState

    data class Content(
        val habit: Habit,
        val area: LifeArea?,
        val streak: StreakInfo,
        val score30: Int?,
        val trend: Trend,
        val days: List<DayCell>,
        val weeks: List<WeekPoint>,
        val notes: List<HabitCheckIn>,
    ) : HabitDetailUiState
}

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    areaRepository: LifeAreaRepository,
    checkInRepository: CheckInRepository,
    engine: ScoreEngine,
    clock: Clock,
) : ViewModel() {

    private val route: HabitDetailRoute = savedStateHandle.toRoute()
    private val today: LocalDate = LocalDate.now(clock)

    val uiState: StateFlow<HabitDetailUiState> = combine(
        habitRepository.observeHabit(route.habitId),
        areaRepository.observeAreas(),
        checkInRepository.observeForHabit(route.habitId),
    ) { habit, areas, checkIns ->
        if (habit == null) {
            HabitDetailUiState.NotFound
        } else {
            HabitDetailUiState.Content(
                habit = habit,
                area = areas.firstOrNull { it.id == habit.areaId },
                streak = engine.streak(habit, checkIns, today),
                score30 = engine.habitScore(habit, checkIns, engine.scoreWindowStart(today), today),
                trend = engine.trend(listOf(habit), checkIns, today),
                days = engine.last14Days(habit, checkIns, today),
                weeks = engine.weeklySeries(habit, checkIns, today),
                notes = checkIns
                    .filter { !it.note.isNullOrBlank() }
                    .sortedByDescending { it.date }
                    .take(10),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState.Loading)

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    fun requestDelete() {
        _showDeleteDialog.value = true
    }

    fun dismissDelete() {
        _showDeleteDialog.value = false
    }

    fun confirmDelete() {
        _showDeleteDialog.value = false
        viewModelScope.launch { habitRepository.delete(route.habitId) }
    }
}

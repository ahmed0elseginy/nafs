package com.nafs.app.presentation.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.model.LifeArea
import com.nafs.app.domain.repository.CheckInRepository
import com.nafs.app.domain.repository.HabitRepository
import com.nafs.app.domain.repository.LifeAreaRepository
import com.nafs.app.domain.scoring.ScoreEngine
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

sealed interface HabitsUiState {
    data object Loading : HabitsUiState
    data class Content(val sections: List<AreaSection>) : HabitsUiState
}

data class AreaSection(val area: LifeArea, val habits: List<HabitRow>)

data class HabitRow(val habit: Habit, val score: Int?)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    areaRepository: LifeAreaRepository,
    checkInRepository: CheckInRepository,
    engine: ScoreEngine,
    clock: Clock,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now(clock)
    private val windowStart: LocalDate = engine.scoreWindowStart(today)

    val uiState: StateFlow<HabitsUiState> = combine(
        habitRepository.observeHabits(),
        areaRepository.observeAreas(),
        checkInRepository.observeInRange(windowStart, today),
    ) { habits, areas, checkIns ->
        val sections = areas.mapNotNull { area ->
            val rows = habits
                .filter { it.areaId == area.id }
                .map { habit -> HabitRow(habit, engine.habitScore(habit, checkIns, windowStart, today)) }
            if (rows.isEmpty()) null else AreaSection(area, rows)
        }
        HabitsUiState.Content(sections)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState.Loading)

    private val _pendingDelete = MutableStateFlow<Habit?>(null)
    val pendingDelete: StateFlow<Habit?> = _pendingDelete.asStateFlow()

    fun requestDelete(habit: Habit) {
        _pendingDelete.value = habit
    }

    fun dismissDelete() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val habit = _pendingDelete.value ?: return
        viewModelScope.launch { habitRepository.delete(habit.id) }
        _pendingDelete.value = null
    }
}

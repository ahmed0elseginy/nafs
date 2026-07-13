package com.nafs.app.presentation.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.model.HabitFrequency
import com.nafs.app.domain.repository.CheckInRepository
import com.nafs.app.domain.repository.HabitRepository
import com.nafs.app.domain.repository.LifeAreaRepository
import com.nafs.app.domain.scoring.HabitScheduler
import com.nafs.app.domain.scoring.weekStart
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DailyCheckInUiState {
    data object Loading : DailyCheckInUiState

    data class Content(
        val date: LocalDate,
        val items: List<CheckInItem>,
    ) : DailyCheckInUiState {
        val completedCount: Int get() = items.count { it.completed }
        val dayScore: Int? get() = if (items.isEmpty()) null else 100 * completedCount / items.size
    }
}

data class CheckInItem(
    val habit: Habit,
    val areaName: String,
    val areaColor: Long,
    val completed: Boolean,
    val note: String?,
    /** done-to-quota for weekly habits, null otherwise. */
    val weeklyProgress: Pair<Int, Int>?,
)

data class NoteDraft(val habitId: Long, val text: String)

@HiltViewModel
class DailyCheckInViewModel @Inject constructor(
    habitRepository: HabitRepository,
    areaRepository: LifeAreaRepository,
    private val checkInRepository: CheckInRepository,
    private val scheduler: HabitScheduler,
    clock: Clock,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now(clock)

    val uiState: StateFlow<DailyCheckInUiState> = combine(
        habitRepository.observeHabits(),
        areaRepository.observeAreas(),
        // The whole week is needed so weekly-quota habits know their progress.
        checkInRepository.observeInRange(today.weekStart(), today),
    ) { habits, areas, weekCheckIns ->
        val areasById = areas.associateBy { it.id }
        val todayByHabit = weekCheckIns.filter { it.date == today }.associateBy { it.habitId }
        val items = habits
            .filter { scheduler.isDueOn(it, today, weekCheckIns) }
            .map { habit ->
                val checkIn = todayByHabit[habit.id]
                val area = areasById[habit.areaId]
                CheckInItem(
                    habit = habit,
                    areaName = area?.name.orEmpty(),
                    areaColor = area?.colorArgb ?: FALLBACK_AREA_COLOR,
                    completed = checkIn?.completed == true,
                    note = checkIn?.note,
                    weeklyProgress = (habit.frequency as? HabitFrequency.Weekly)?.let { weekly ->
                        val done = weekCheckIns.count { it.habitId == habit.id && it.completed }
                        done to weekly.timesPerWeek
                    },
                )
            }
        DailyCheckInUiState.Content(date = today, items = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyCheckInUiState.Loading)

    private val _noteDraft = MutableStateFlow<NoteDraft?>(null)
    val noteDraft: StateFlow<NoteDraft?> = _noteDraft.asStateFlow()

    fun toggle(item: CheckInItem) {
        viewModelScope.launch {
            checkInRepository.setCompleted(item.habit.id, today, !item.completed)
        }
    }

    fun openNote(item: CheckInItem) {
        _noteDraft.value = NoteDraft(item.habit.id, item.note.orEmpty())
    }

    fun updateNote(text: String) {
        _noteDraft.update { it?.copy(text = text) }
    }

    fun saveNote() {
        val draft = _noteDraft.value ?: return
        viewModelScope.launch { checkInRepository.setNote(draft.habitId, today, draft.text) }
        _noteDraft.value = null
    }

    fun dismissNote() {
        _noteDraft.value = null
    }

    private companion object {
        const val FALLBACK_AREA_COLOR = 0xFF546E7AL
    }
}

package com.nafs.app.presentation.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.model.HabitFrequency
import com.nafs.app.domain.model.LifeArea
import com.nafs.app.domain.repository.HabitRepository
import com.nafs.app.domain.repository.LifeAreaRepository
import com.nafs.app.presentation.navigation.HabitEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI-level frequency selector; maps to [HabitFrequency] on save. */
enum class FrequencyOption { DAILY, WEEKLY, SPECIFIC_DAYS }

data class HabitFormState(
    val name: String = "",
    val nameError: Boolean = false,
    val description: String = "",
    val areaId: Long = 0,
    val areaError: Boolean = false,
    val frequencyOption: FrequencyOption = FrequencyOption.DAILY,
    val timesPerWeek: Int = 3,
    val days: Set<DayOfWeek> = emptySet(),
    val daysError: Boolean = false,
    val startDate: LocalDate,
    val openEnded: Boolean = true,
    val durationText: String = "30",
    val durationError: Boolean = false,
    val targetScore: Int = 100,
    val reminderTime: LocalTime? = null,
)

@HiltViewModel
class HabitEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    areaRepository: LifeAreaRepository,
    clock: Clock,
) : ViewModel() {

    private val route: HabitEditorRoute = savedStateHandle.toRoute()

    val isNew: Boolean = route.habitId == 0L

    val areas: StateFlow<List<LifeArea>> = areaRepository.observeAreas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(HabitFormState(startDate = LocalDate.now(clock)))
    val form: StateFlow<HabitFormState> = _form.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch {
            if (isNew) {
                val defaultAreaId = if (route.areaId != 0L) {
                    route.areaId
                } else {
                    areaRepository.observeAreas().first().firstOrNull()?.id ?: 0L
                }
                _form.update { it.copy(areaId = defaultAreaId) }
            } else {
                habitRepository.observeHabit(route.habitId).first()?.let { habit ->
                    _form.value = habit.toFormState()
                }
            }
        }
    }

    fun onNameChange(name: String) = _form.update { it.copy(name = name, nameError = false) }

    fun onDescriptionChange(description: String) = _form.update { it.copy(description = description) }

    fun onAreaChange(areaId: Long) = _form.update { it.copy(areaId = areaId, areaError = false) }

    fun onFrequencyChange(option: FrequencyOption) = _form.update { it.copy(frequencyOption = option) }

    fun onTimesPerWeekChange(delta: Int) = _form.update {
        it.copy(timesPerWeek = (it.timesPerWeek + delta).coerceIn(1, 7))
    }

    fun toggleDay(day: DayOfWeek) = _form.update {
        val days = if (day in it.days) it.days - day else it.days + day
        it.copy(days = days, daysError = false)
    }

    fun onStartDateChange(date: LocalDate) = _form.update { it.copy(startDate = date) }

    fun onOpenEndedChange(openEnded: Boolean) = _form.update { it.copy(openEnded = openEnded) }

    fun onDurationChange(text: String) = _form.update {
        it.copy(durationText = text.filter(Char::isDigit).take(4), durationError = false)
    }

    fun onTargetScoreChange(value: Int) = _form.update { it.copy(targetScore = value) }

    fun onReminderChange(time: LocalTime?) = _form.update { it.copy(reminderTime = time) }

    fun save() {
        val state = _form.value
        val duration = state.durationText.toIntOrNull()
        val nameError = state.name.isBlank()
        val areaError = state.areaId == 0L
        val daysError = state.frequencyOption == FrequencyOption.SPECIFIC_DAYS && state.days.isEmpty()
        val durationError = !state.openEnded && (duration == null || duration <= 0)
        if (nameError || areaError || daysError || durationError) {
            _form.update {
                it.copy(
                    nameError = nameError,
                    areaError = areaError,
                    daysError = daysError,
                    durationError = durationError,
                )
            }
            return
        }

        val frequency = when (state.frequencyOption) {
            FrequencyOption.DAILY -> HabitFrequency.Daily
            FrequencyOption.WEEKLY -> HabitFrequency.Weekly(state.timesPerWeek)
            FrequencyOption.SPECIFIC_DAYS -> HabitFrequency.SpecificDays(state.days)
        }
        viewModelScope.launch {
            habitRepository.upsert(
                Habit(
                    id = route.habitId,
                    areaId = state.areaId,
                    name = state.name.trim(),
                    description = state.description.trim().ifEmpty { null },
                    frequency = frequency,
                    startDate = state.startDate,
                    durationDays = if (state.openEnded) null else duration,
                    targetScore = state.targetScore,
                    reminderTime = state.reminderTime,
                ),
            )
            _saved.value = true
        }
    }

    private fun Habit.toFormState() = HabitFormState(
        name = name,
        description = description.orEmpty(),
        areaId = areaId,
        frequencyOption = when (frequency) {
            HabitFrequency.Daily -> FrequencyOption.DAILY
            is HabitFrequency.Weekly -> FrequencyOption.WEEKLY
            is HabitFrequency.SpecificDays -> FrequencyOption.SPECIFIC_DAYS
        },
        timesPerWeek = (frequency as? HabitFrequency.Weekly)?.timesPerWeek ?: 3,
        days = (frequency as? HabitFrequency.SpecificDays)?.days ?: emptySet(),
        startDate = startDate,
        openEnded = durationDays == null,
        durationText = durationDays?.toString() ?: "30",
        targetScore = targetScore,
        reminderTime = reminderTime,
    )
}

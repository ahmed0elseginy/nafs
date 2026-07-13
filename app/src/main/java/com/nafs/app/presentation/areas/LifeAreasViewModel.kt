package com.nafs.app.presentation.areas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafs.app.domain.model.LifeArea
import com.nafs.app.domain.model.Trend
import com.nafs.app.domain.repository.CheckInRepository
import com.nafs.app.domain.repository.HabitRepository
import com.nafs.app.domain.repository.LifeAreaRepository
import com.nafs.app.domain.scoring.ScoreEngine
import com.nafs.app.presentation.theme.AreaColorOptions
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

sealed interface LifeAreasUiState {
    data object Loading : LifeAreasUiState
    data class Content(val cards: List<AreaCard>) : LifeAreasUiState
}

data class AreaCard(
    val area: LifeArea,
    val habitCount: Int,
    val score: Int?,
    val trend: Trend,
)

data class AreaEditorState(
    val id: Long = 0,
    val name: String = "",
    val iconKey: String = "star",
    val colorArgb: Long = AreaColorOptions.first(),
    val weight: Float = 1f,
    val position: Int = 0,
    val nameError: Boolean = false,
) {
    val isNew: Boolean get() = id == 0L
}

@HiltViewModel
class LifeAreasViewModel @Inject constructor(
    private val areaRepository: LifeAreaRepository,
    habitRepository: HabitRepository,
    checkInRepository: CheckInRepository,
    private val engine: ScoreEngine,
    clock: Clock,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now(clock)
    private val windowStart: LocalDate = engine.scoreWindowStart(today)

    val uiState: StateFlow<LifeAreasUiState> = combine(
        areaRepository.observeAreas(),
        habitRepository.observeHabits(),
        checkInRepository.observeInRange(windowStart, today),
    ) { areas, habits, checkIns ->
        val cards = areas.map { area ->
            val areaHabits = habits.filter { it.areaId == area.id }
            AreaCard(
                area = area,
                habitCount = areaHabits.size,
                score = engine.areaScore(areaHabits, checkIns, windowStart, today),
                trend = engine.trend(areaHabits, checkIns, today),
            )
        }
        LifeAreasUiState.Content(cards)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LifeAreasUiState.Loading)

    private val _editor = MutableStateFlow<AreaEditorState?>(null)
    val editor: StateFlow<AreaEditorState?> = _editor.asStateFlow()

    private val _pendingDelete = MutableStateFlow<LifeArea?>(null)
    val pendingDelete: StateFlow<LifeArea?> = _pendingDelete.asStateFlow()

    fun openNewEditor() {
        _editor.value = AreaEditorState()
    }

    fun openEditor(area: LifeArea) {
        _editor.value = AreaEditorState(
            id = area.id,
            name = area.name,
            iconKey = area.iconKey,
            colorArgb = area.colorArgb,
            weight = area.weight,
            position = area.position,
        )
    }

    fun onNameChange(name: String) = _editor.update { it?.copy(name = name, nameError = false) }

    fun onIconChange(iconKey: String) = _editor.update { it?.copy(iconKey = iconKey) }

    fun onColorChange(colorArgb: Long) = _editor.update { it?.copy(colorArgb = colorArgb) }

    fun onWeightChange(weight: Float) = _editor.update { it?.copy(weight = weight) }

    fun dismissEditor() {
        _editor.value = null
    }

    fun saveEditor() {
        val editor = _editor.value ?: return
        if (editor.name.isBlank()) {
            _editor.update { it?.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            areaRepository.upsert(
                LifeArea(
                    id = editor.id,
                    name = editor.name.trim(),
                    iconKey = editor.iconKey,
                    colorArgb = editor.colorArgb,
                    weight = editor.weight,
                    position = editor.position,
                ),
            )
        }
        _editor.value = null
    }

    /** Called from the editor sheet; deletion cascades to habits, so it must be confirmed. */
    fun requestDelete() {
        val editor = _editor.value ?: return
        if (editor.isNew) return
        _pendingDelete.value = LifeArea(
            id = editor.id,
            name = editor.name,
            iconKey = editor.iconKey,
            colorArgb = editor.colorArgb,
            weight = editor.weight,
            position = editor.position,
        )
    }

    fun dismissDelete() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val area = _pendingDelete.value ?: return
        viewModelScope.launch { areaRepository.delete(area.id) }
        _pendingDelete.value = null
        _editor.value = null
    }
}

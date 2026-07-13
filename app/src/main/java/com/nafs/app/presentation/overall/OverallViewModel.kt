package com.nafs.app.presentation.overall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafs.app.domain.model.LifeArea
import com.nafs.app.domain.model.Trend
import com.nafs.app.domain.model.WeekPoint
import com.nafs.app.domain.repository.CheckInRepository
import com.nafs.app.domain.repository.HabitRepository
import com.nafs.app.domain.repository.LifeAreaRepository
import com.nafs.app.domain.scoring.ScoreEngine
import com.nafs.app.domain.scoring.weekStart
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface OverallUiState {
    data object Loading : OverallUiState
    data object Empty : OverallUiState
    data class Content(
        val overallScore: Int?,
        val trend: Trend,
        val weekly: List<WeekPoint>,
        val areas: List<AreaBreakdown>,
        val areaCount: Int,
        val habitCount: Int,
    ) : OverallUiState
}

data class AreaBreakdown(
    val area: LifeArea,
    val score: Int?,
    val trend: Trend,
)

@HiltViewModel
class OverallViewModel @Inject constructor(
    areaRepository: LifeAreaRepository,
    habitRepository: HabitRepository,
    checkInRepository: CheckInRepository,
    private val engine: ScoreEngine,
    clock: Clock,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now(clock)
    private val windowStart: LocalDate = engine.scoreWindowStart(today)
    private val chartStart: LocalDate = today.weekStart().minusWeeks((CHART_WEEKS - 1).toLong())

    val uiState: StateFlow<OverallUiState> = combine(
        areaRepository.observeAreas(),
        habitRepository.observeHabits(),
        checkInRepository.observeInRange(chartStart, today),
    ) { areas, habits, checkIns ->
        if (areas.isEmpty()) return@combine OverallUiState.Empty

        val habitsByArea = habits.groupBy { it.areaId }
        val breakdown = areas.map { area ->
            val areaHabits = habitsByArea[area.id].orEmpty()
            AreaBreakdown(
                area = area,
                score = engine.areaScore(areaHabits, checkIns, windowStart, today),
                trend = engine.trend(areaHabits, checkIns, today),
            )
        }

        OverallUiState.Content(
            overallScore = engine.overallScoreAt(areas, habits, checkIns, windowStart, today),
            trend = engine.overallTrend(areas, habits, checkIns, today),
            weekly = engine.overallWeeklySeries(areas, habits, checkIns, today, CHART_WEEKS),
            areas = breakdown,
            areaCount = areas.size,
            habitCount = habits.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverallUiState.Loading)

    private companion object {
        const val CHART_WEEKS = 10
    }
}

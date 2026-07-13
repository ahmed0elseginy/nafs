package com.nafs.app.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nafs.app.R
import com.nafs.app.domain.model.HabitFrequency
import com.nafs.app.domain.scoring.WEEK_START
import java.time.format.TextStyle
import java.util.Locale

/** Human-readable frequency, with specific days ordered Saturday-first. */
@Composable
fun frequencyLabel(frequency: HabitFrequency): String = when (frequency) {
    HabitFrequency.Daily -> stringResource(R.string.freq_daily)
    is HabitFrequency.Weekly -> stringResource(R.string.freq_weekly_label, frequency.timesPerWeek)
    is HabitFrequency.SpecificDays -> frequency.days
        .sortedBy { (it.value - WEEK_START.value + 7) % 7 }
        .joinToString(separator = " · ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

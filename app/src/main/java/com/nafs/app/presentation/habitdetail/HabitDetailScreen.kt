package com.nafs.app.presentation.habitdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nafs.app.R
import com.nafs.app.domain.model.DayCell
import com.nafs.app.domain.model.StreakInfo
import com.nafs.app.domain.model.StreakUnit
import com.nafs.app.presentation.common.TrendIcon
import com.nafs.app.presentation.common.frequencyLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsStateWithLifecycle()
    val content = uiState as? HabitDetailUiState.Content

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = content?.habit?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    content?.let { state ->
                        IconButton(onClick = { onEdit(state.habit.id) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = viewModel::requestDelete) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            HabitDetailUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            HabitDetailUiState.NotFound -> Box(modifier = Modifier.fillMaxSize().padding(padding))

            is HabitDetailUiState.Content -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HabitHeader(state)
                StatRow(state)
                DayStrip(state.days)
                Column {
                    Text(
                        text = stringResource(R.string.detail_weekly_chart),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    WeeklyCompletionChart(
                        points = state.weeks,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                }
                NotesSection(state)
            }
        }
    }

    if (showDeleteDialog && content != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(stringResource(R.string.habit_delete_title, content.habit.name)) },
            text = { Text(stringResource(R.string.habit_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDelete()
                        onBack()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun HabitHeader(state: HabitDetailUiState.Content) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            state.area?.let { area ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(area.colorArgb), CircleShape),
                )
                Text(
                    text = area.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                )
                Text(
                    text = "·",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(
                text = frequencyLabel(state.habit.frequency),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.habit.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        val dates = buildString {
            append(stringResource(R.string.detail_started, formatDate(state.habit.startDate)))
            state.habit.endDate?.let { end ->
                append(" · ")
                append(stringResource(R.string.detail_ends, formatDate(end)))
            }
        }
        Text(
            text = dates,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun StatRow(state: HabitDetailUiState.Content) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            label = stringResource(R.string.detail_streak),
            value = streakText(state.streak, state.streak.current),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.detail_best_streak),
            value = streakText(state.streak, state.streak.best),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.detail_rate_30d),
            value = state.score30?.let { stringResource(R.string.score_percent, it) } ?: "—",
            trailing = { TrendIcon(trend = state.trend, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                trailing?.let {
                    Box(modifier = Modifier.padding(start = 4.dp)) { it() }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun DayStrip(days: List<DayCell>) {
    Column {
        Text(
            text = stringResource(R.string.detail_last_14_days),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { cell ->
                val dotModifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                Box(
                    modifier = when {
                        cell.completed -> dotModifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        cell.scheduled -> dotModifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        else -> dotModifier.background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun NotesSection(state: HabitDetailUiState.Content) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.detail_notes),
            style = MaterialTheme.typography.titleSmall,
        )
        if (state.notes.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_no_notes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.notes.forEach { checkIn ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = formatDate(checkIn.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = checkIn.note.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun streakText(streak: StreakInfo, count: Int): String = when (streak.unit) {
    StreakUnit.DAYS -> pluralStringResource(R.plurals.streak_days, count, count)
    StreakUnit.WEEKS -> pluralStringResource(R.plurals.streak_weeks, count, count)
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))

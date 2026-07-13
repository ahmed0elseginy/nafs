package com.nafs.app.presentation.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nafs.app.R
import com.nafs.app.presentation.common.EmptyState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCheckInScreen(
    onHabitClick: (Long) -> Unit,
    viewModel: DailyCheckInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val noteDraft by viewModel.noteDraft.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nav_today))
                        (uiState as? DailyCheckInUiState.Content)?.let { content ->
                            Text(
                                text = formatFullDate(content.date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            DailyCheckInUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is DailyCheckInUiState.Content -> if (state.items.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Spa,
                    title = stringResource(R.string.today_empty_title),
                    message = stringResource(R.string.today_empty_message),
                    modifier = Modifier.padding(padding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { DaySummaryCard(state) }
                    items(state.items, key = { it.habit.id }) { item ->
                        CheckInRow(
                            item = item,
                            onClick = { onHabitClick(item.habit.id) },
                            onToggle = { viewModel.toggle(item) },
                            onNoteClick = { viewModel.openNote(item) },
                        )
                    }
                }
            }
        }
    }

    noteDraft?.let { draft ->
        AlertDialog(
            onDismissRequest = viewModel::dismissNote,
            title = { Text(stringResource(R.string.note_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = draft.text,
                    onValueChange = viewModel::updateNote,
                    placeholder = { Text(stringResource(R.string.note_dialog_hint)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveNote) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissNote) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun DaySummaryCard(state: DailyCheckInUiState.Content) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    R.string.today_progress_format,
                    state.completedCount,
                    state.items.size,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LinearProgressIndicator(
                progress = { if (state.items.isEmpty()) 0f else state.completedCount.toFloat() / state.items.size },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            state.dayScore?.let { score ->
                Text(
                    text = stringResource(R.string.today_day_score, score),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CheckInRow(
    item: CheckInItem,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onNoteClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(item.areaColor), CircleShape),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = item.habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                    color = if (item.completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                val subtitle = buildString {
                    append(item.areaName)
                    item.weeklyProgress?.let { (done, quota) -> append(" · $done/$quota") }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!item.note.isNullOrBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onNoteClick) {
                Icon(
                    imageVector = Icons.Rounded.EditNote,
                    contentDescription = stringResource(
                        if (item.note.isNullOrBlank()) R.string.checkin_note_add else R.string.checkin_note_edit,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(checked = item.completed, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun formatFullDate(date: LocalDate): String {
    val locale = Locale.getDefault()
    return remember(date, locale) {
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }
}

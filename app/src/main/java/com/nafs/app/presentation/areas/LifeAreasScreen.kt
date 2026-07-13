package com.nafs.app.presentation.areas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nafs.app.R
import com.nafs.app.presentation.common.EmptyState
import com.nafs.app.presentation.common.NafsIcons
import com.nafs.app.presentation.common.ScoreRing
import com.nafs.app.presentation.common.TrendIcon
import com.nafs.app.presentation.theme.AreaColorOptions
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeAreasScreen(viewModel: LifeAreasViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.areas_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openNewEditor) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.area_add))
            }
        },
    ) { padding ->
        when (val state = uiState) {
            LifeAreasUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is LifeAreasUiState.Content -> if (state.cards.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Workspaces,
                    title = stringResource(R.string.areas_empty_title),
                    message = stringResource(R.string.areas_empty_message),
                    modifier = Modifier.padding(padding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.cards, key = { it.area.id }) { card ->
                        AreaCardRow(card = card, onClick = { viewModel.openEditor(card.area) })
                    }
                }
            }
        }
    }

    editor?.let { state ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissEditor,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            AreaEditorSheet(state = state, viewModel = viewModel)
        }
    }

    pendingDelete?.let { area ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(stringResource(R.string.area_delete_title, area.name)) },
            text = { Text(stringResource(R.string.area_delete_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
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
private fun AreaCardRow(card: AreaCard, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(card.area.colorArgb).copy(alpha = 0.15f), CircleShape),
            ) {
                Icon(
                    imageVector = NafsIcons.forKey(card.area.iconKey),
                    contentDescription = null,
                    tint = Color(card.area.colorArgb),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(card.area.name, style = MaterialTheme.typography.titleMedium)
                val subtitle = buildString {
                    append(pluralStringResource(R.plurals.habit_count, card.habitCount, card.habitCount))
                    if (card.area.weight != 1f) {
                        append(" · ")
                        append(stringResource(R.string.area_weight_value, formatWeight(card.area.weight)))
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TrendIcon(trend = card.trend, modifier = Modifier.padding(end = 10.dp))
            ScoreRing(score = card.score)
        }
    }
}

@Composable
private fun AreaEditorSheet(state: AreaEditorState, viewModel: LifeAreasViewModel) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .imePadding(),
    ) {
        Text(
            text = stringResource(if (state.isNew) R.string.area_add else R.string.area_edit_title),
            style = MaterialTheme.typography.titleLarge,
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.area_name_label)) },
            isError = state.nameError,
            supportingText = if (state.nameError) {
                { Text(stringResource(R.string.error_name_required)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        Text(
            text = stringResource(R.string.area_icon_label),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        )
        NafsIcons.options.chunked(7).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                rowOptions.forEach { (key, vector) ->
                    val selected = key == state.iconKey
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                shape = CircleShape,
                            )
                            .clickable { viewModel.onIconChange(key) },
                    ) {
                        Icon(
                            imageVector = vector,
                            contentDescription = key,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.area_color_label),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        )
        AreaColorOptions.chunked(6).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                rowColors.forEach { colorValue ->
                    val selected = colorValue == state.colorArgb
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(colorValue), CircleShape)
                            .clickable { viewModel.onColorChange(colorValue) },
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.area_weight_label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.area_weight_value, formatWeight(state.weight)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = state.weight,
            onValueChange = viewModel::onWeightChange,
            valueRange = 0.5f..3f,
            steps = 9,
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            if (!state.isNew) {
                TextButton(onClick = viewModel::requestDelete) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.weight(1f),
            ) {
                TextButton(onClick = viewModel::dismissEditor) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(onClick = viewModel::saveEditor) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

private fun formatWeight(weight: Float): String {
    val rounded = (weight * 100).roundToInt() / 100f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
}

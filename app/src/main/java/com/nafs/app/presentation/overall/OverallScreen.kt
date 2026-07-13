package com.nafs.app.presentation.overall

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
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nafs.app.R
import com.nafs.app.presentation.common.EmptyState
import com.nafs.app.presentation.common.NafsIcons
import com.nafs.app.presentation.common.ScoreRing
import com.nafs.app.presentation.common.ScoreTrendChart
import com.nafs.app.presentation.common.TrendIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverallScreen(
    onSettingsClick: () -> Unit,
    viewModel: OverallViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.overall_title)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            OverallUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            OverallUiState.Empty -> EmptyState(
                icon = Icons.Rounded.Insights,
                title = stringResource(R.string.overall_empty_title),
                message = stringResource(R.string.overall_empty_message),
                modifier = Modifier.padding(padding),
            )

            is OverallUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { OverallHeroCard(state) }
                item { TrendChartCard(state) }
                item {
                    Text(
                        text = stringResource(R.string.overall_by_area),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    )
                }
                items(state.areas, key = { it.area.id }) { breakdown ->
                    AreaBreakdownRow(breakdown)
                }
            }
        }
    }
}

@Composable
private fun OverallHeroCard(state: OverallUiState.Content) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp),
        ) {
            ScoreRing(score = state.overallScore, size = 88.dp)
            Column(modifier = Modifier.padding(start = 20.dp)) {
                Text(
                    text = stringResource(R.string.overall_status_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    TrendIcon(trend = state.trend)
                    Text(
                        text = stringResource(R.string.overall_window_caption),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.overall_counts,
                        state.areaCount,
                        state.habitCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TrendChartCard(state: OverallUiState.Content) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.overall_chart_title, state.weekly.size),
                style = MaterialTheme.typography.titleMedium,
            )
            ScoreTrendChart(
                points = state.weekly.map { it.score },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun AreaBreakdownRow(breakdown: AreaBreakdown) {
    val areaColor = Color(breakdown.area.colorArgb)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(areaColor.copy(alpha = 0.15f), CircleShape),
            ) {
                Icon(
                    imageVector = NafsIcons.forKey(breakdown.area.iconKey),
                    contentDescription = null,
                    tint = areaColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(breakdown.area.name, style = MaterialTheme.typography.titleSmall)
                LinearProgressIndicator(
                    progress = { (breakdown.score ?: 0) / 100f },
                    color = areaColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            TrendIcon(trend = breakdown.trend, modifier = Modifier.padding(end = 10.dp))
            Text(
                text = breakdown.score?.toString() ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

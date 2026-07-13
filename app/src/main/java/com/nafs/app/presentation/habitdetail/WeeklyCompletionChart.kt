package com.nafs.app.presentation.habitdetail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nafs.app.R
import com.nafs.app.domain.model.WeekPoint
import com.nafs.app.presentation.common.ScoreTrendChart

/**
 * Per-week completion scores on the habit detail screen. Weeks without
 * expectations (before the habit started) become gaps in the line; with fewer
 * than two scorable weeks a quiet placeholder is shown instead of a chart.
 */
@Composable
fun WeeklyCompletionChart(points: List<WeekPoint>, modifier: Modifier = Modifier) {
    if (points.count { it.score != null } < 2) {
        Text(
            text = stringResource(R.string.detail_not_enough_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    ScoreTrendChart(points = points.map { it.score }, modifier = modifier)
}

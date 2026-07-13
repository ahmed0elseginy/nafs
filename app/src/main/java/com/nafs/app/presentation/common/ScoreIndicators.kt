package com.nafs.app.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nafs.app.R
import com.nafs.app.domain.model.Trend

/** Circular 0–100 score. Renders an em dash when the score is null (nothing expected yet). */
@Composable
fun ScoreRing(
    score: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        CircularProgressIndicator(
            progress = { (score ?: 0) / 100f },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 4.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = score?.toString() ?: "—",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun TrendIcon(trend: Trend, modifier: Modifier = Modifier) {
    when (trend) {
        Trend.UP -> Icon(
            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
            contentDescription = stringResource(R.string.trend_up),
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier,
        )
        Trend.DOWN -> Icon(
            imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
            contentDescription = stringResource(R.string.trend_down),
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        Trend.STEADY -> Icon(
            imageVector = Icons.AutoMirrored.Rounded.TrendingFlat,
            contentDescription = stringResource(R.string.trend_steady),
            tint = MaterialTheme.colorScheme.outline,
            modifier = modifier,
        )
    }
}

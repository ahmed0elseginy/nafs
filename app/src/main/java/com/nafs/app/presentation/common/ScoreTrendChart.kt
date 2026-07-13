package com.nafs.app.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A lightweight line chart of 0–100 scores drawn directly on a [Canvas]: a smooth
 * value line over a soft gradient area, with point markers and a 0/50/100 axis.
 * Nulls break the line into separate segments (weeks with nothing expected).
 *
 * Intentionally dependency-free so the same visual is reused by the habit detail
 * chart and the Overall tab without fighting a charting library's API.
 */
@Composable
fun ScoreTrendChart(
    points: List<Int?>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 168.dp,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val markerRing = MaterialTheme.colorScheme.surface
    val axisStyle = MaterialTheme.typography.labelSmall
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(modifier = modifier.height(height)) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
        ) {
            listOf(100, 50, 0).forEach { label ->
                Text(text = label.toString(), style = axisStyle, color = axisColor)
            }
        }

        Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val w = size.width
            val h = size.height
            fun xAt(index: Int): Float =
                if (points.size <= 1) w / 2f else w * index / (points.size - 1)
            fun yAt(value: Int): Float = h * (1f - value / 100f)

            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = h * fraction
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            for (segment in segments(points)) {
                if (segment.size >= 2) {
                    val line = Path().apply {
                        moveTo(xAt(segment.first().index), yAt(segment.first().value))
                        segment.drop(1).forEach { lineTo(xAt(it.index), yAt(it.value)) }
                    }
                    val area = Path().apply {
                        addPath(line)
                        lineTo(xAt(segment.last().index), h)
                        lineTo(xAt(segment.first().index), h)
                        close()
                    }
                    drawPath(
                        path = area,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f)),
                            startY = 0f,
                            endY = h,
                        ),
                    )
                    drawPath(
                        path = line,
                        color = lineColor,
                        style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                for (p in segment) {
                    val center = Offset(xAt(p.index), yAt(p.value))
                    drawCircle(color = markerRing, radius = 4.5f.dp.toPx(), center = center)
                    drawCircle(color = lineColor, radius = 3f.dp.toPx(), center = center)
                }
            }
        }
    }
}

private data class ChartPoint(val index: Int, val value: Int)

/** Splits the series into runs of consecutive non-null values. */
private fun segments(points: List<Int?>): List<List<ChartPoint>> {
    val result = mutableListOf<MutableList<ChartPoint>>()
    var current: MutableList<ChartPoint>? = null
    points.forEachIndexed { index, value ->
        if (value == null) {
            current = null
        } else {
            val run = current ?: mutableListOf<ChartPoint>().also { result += it; current = it }
            run += ChartPoint(index, value)
        }
    }
    return result
}

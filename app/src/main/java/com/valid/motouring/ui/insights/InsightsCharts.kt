package com.valid.motouring.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.valid.motouring.simulation.VehicleSplit
import com.valid.motouring.simulation.WeekDistance
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted
import kotlin.math.roundToInt

@Composable
fun WeeklyDistanceChart(weeks: List<WeekDistance>, modifier: Modifier = Modifier) {
    if (weeks.isEmpty()) {
        Text("No rides yet", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
        return
    }
    val maxKm = weeks.maxOf { it.distanceKm }.coerceAtLeast(1.0)
    Column(modifier.fillMaxWidth()) {
        Text("Peak ${maxKm.roundToInt()} km / week", color = Muted, style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(140.dp).padding(top = 6.dp)) {
            val n = weeks.size
            val gap = 6.dp.toPx()
            val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(2f)
            val topPad = 8.dp.toPx()
            drawLine(Charcoal600, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            weeks.forEachIndexed { i, w ->
                val h = (w.distanceKm / maxKm * (size.height - topPad)).toFloat()
                val x = i * (barW + gap)
                if (h > 0f) {
                    drawRoundRect(
                        color = MotouringColors.goal,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleSplitBar(split: VehicleSplit, modifier: Modifier = Modifier) {
    val total = (split.motoKm + split.carKm).coerceAtLeast(0.001)
    val motoPct = (split.motoKm / total * 100).roundToInt()
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
            if (split.motoKm > 0) Box(Modifier.weight(split.motoKm.toFloat()).fillMaxHeight().background(MotouringColors.rider))
            if (split.motoKm > 0 && split.carKm > 0) Spacer(Modifier.width(2.dp))
            if (split.carKm > 0) Box(Modifier.weight(split.carKm.toFloat()).fillMaxHeight().background(MotouringColors.poiRest))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Moto ${split.motoKm.roundToInt()} km · ${motoPct}%", color = MotouringColors.rider, style = MaterialTheme.typography.labelMedium)
            Text("Car ${split.carKm.roundToInt()} km · ${100 - motoPct}%", color = MotouringColors.poiRest, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ScoreTrendSparkline(scores: List<Int>, modifier: Modifier = Modifier) {
    if (scores.size < 2) {
        Text("Not enough rides yet", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
        return
    }
    val minS = scores.min()
    val range = (scores.max() - minS).coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        Text("Latest ${scores.last()}", color = Muted, style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(64.dp).padding(top = 6.dp)) {
            val n = scores.size
            val dx = if (n > 1) size.width / (n - 1) else 0f
            val pad = 6.dp.toPx()
            fun pt(i: Int): Offset {
                val v = (scores[i] - minS).toFloat() / range
                return Offset(i * dx, size.height - pad - v * (size.height - pad * 2))
            }
            for (i in 0 until n - 1) {
                drawLine(MotouringColors.goal, pt(i), pt(i + 1), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            drawCircle(MotouringColors.goal, radius = 5.dp.toPx(), center = pt(n - 1))
        }
    }
}

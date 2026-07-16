package com.valid.motouring.ui.segments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Twistiness
import com.valid.motouring.simulation.sortedByTime
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted

private fun formatTime(seconds: Int) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
fun SegmentsScreen(viewModel: SegmentsViewModel, onSegmentClick: (String) -> Unit) {
    val segments by viewModel.segments.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Segments", style = MaterialTheme.typography.headlineMedium) }
        items(segments, key = { it.id }) { segment ->
            MotouringCard(modifier = Modifier.fillMaxWidth(), onClick = { onSegmentClick(segment.id) }) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(segment.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TwistinessChip(segment.twistiness)
                    }
                    Text("${segment.region} · ${"%.1f".format(segment.lengthKm)} km", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 2.dp))
                    val leader = sortedByTime(segment.leaderboard).firstOrNull()
                    val yours = viewModel.yourBest(segment)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Leader ${leader?.let { formatTime(it.timeSeconds) } ?: "—"}", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text("You ${yours?.let { formatTime(it.timeSeconds) } ?: "no time yet"}", style = MaterialTheme.typography.bodySmall, color = if (yours != null) MotouringColors.goal else Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun TwistinessChip(t: Twistiness) {
    val (label, color) = when (t) {
        Twistiness.MELLOW -> "Mellow" to MotouringColors.statusOk
        Twistiness.FLOWING -> "Flowing" to MotouringColors.statusDueSoon
        Twistiness.TECHNICAL -> "Technical" to MotouringColors.statusOverdue
    }
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

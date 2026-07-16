package com.valid.motouring.ui.segments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.SegmentTime
import com.valid.motouring.ui.theme.Charcoal700
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted

private fun formatTime(seconds: Int) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
fun SegmentDetailScreen(viewModel: SegmentDetailViewModel) {
    val state by viewModel.state.collectAsState()
    val segment = state.segment ?: return
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column {
                Image(
                    painter = painterResource(id = segment.routePreviewRes),
                    contentDescription = segment.name,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.padding(20.dp)) {
                    Text(segment.name, style = MaterialTheme.typography.headlineMedium)
                    Text("${segment.region} · ${"%.1f".format(segment.lengthKm)} km · ${segment.twistiness.name.lowercase()}", style = MaterialTheme.typography.bodyMedium, color = Muted)
                    state.yourRank?.let {
                        Text("Your best: #$it of ${state.rankedBoard.size}", style = MaterialTheme.typography.titleSmall, color = MotouringColors.goal, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Leaderboard", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        itemsIndexed(state.rankedBoard, key = { _, t -> t.userId }) { index, time ->
            LeaderRow(rank = index + 1, time = time, isYou = time.userId == state.currentUserId)
        }
    }
}

@Composable
private fun LeaderRow(rank: Int, time: SegmentTime, isYou: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (isYou) MotouringColors.goal.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$rank", style = MaterialTheme.typography.titleMedium, color = if (rank <= 3) MotouringColors.goal else Muted, modifier = Modifier.width(28.dp))
        Box(Modifier.size(32.dp).clip(CircleShape).background(Charcoal700), contentAlignment = Alignment.Center) {
            Text(time.name.take(1), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.padding(start = 10.dp))
        Text(if (isYou) "${time.name} (you)" else time.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(formatTime(time.timeSeconds), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

package com.valid.motouring.ui.rides

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.activeLegDurationSeconds
import com.valid.motouring.data.model.avgSpeedKmh
import com.valid.motouring.data.model.toGoalMeters
import com.valid.motouring.ui.components.InstrumentRing
import com.valid.motouring.ui.theme.Charcoal700
import com.valid.motouring.ui.theme.Charcoal800
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.MotouringTextStyles
import com.valid.motouring.ui.theme.Muted

private val avatarColors = listOf(
    MotouringColors.rider, MotouringColors.poiRest, MotouringColors.riderPurple, MotouringColors.poiFuel, MotouringColors.riderCoral,
)
private fun colorFor(id: String) = avatarColors[(id.hashCode() and 0x7fffffff) % avatarColors.size]

@Composable
fun RideDashboard(session: RideSession, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val goal = session.activeGoal
            if (session.mode == RideMode.GOAL && goal != null) {
                val progress = (session.distanceMeters / goal.targetDistanceMeters).toFloat().coerceIn(0f, 1f)
                InstrumentRing(progress = progress, size = 72.dp) {
                    Text("%.1f".format(session.toGoalMeters() / 1000.0), style = MotouringTextStyles.statValue)
                    Text("KM LEFT", style = MotouringTextStyles.statLabel, color = Muted)
                }
            } else {
                InstrumentRing(progress = 0f, size = 72.dp) {
                    Text("${session.activeLegDurationSeconds() / 60}", style = MotouringTextStyles.statValue)
                    Text("MIN", style = MotouringTextStyles.statLabel, color = Muted)
                }
            }
            Spacer(Modifier.width(12.dp))
            StatGrid(session, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        GroupBar(session.participants)
    }
}

@Composable
private fun StatGrid(session: RideSession, modifier: Modifier = Modifier) {
    val cells = listOf(
        "Dist" to "%.1f".format(session.distanceMeters / 1000.0),
        "Avg" to "${avgSpeedKmh(session.distanceMeters, session.elapsedSeconds).toInt()}",
        "Time" to "${session.elapsedSeconds / 60}:${(session.elapsedSeconds % 60).toString().padStart(2, '0')}",
        "Max" to "${session.maxSpeedKmh.toInt()}",
        "Climb" to "${session.elevationGainMeters.toInt()}",
        "Goal" to if (session.activeGoal != null) "%.1f".format(session.toGoalMeters() / 1000.0) else "—",
    )
    Column(modifier = modifier.clip(MaterialTheme.shapes.medium).background(Charcoal800).padding(vertical = 6.dp)) {
        cells.chunked(3).forEach { rowCells ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rowCells.forEach { (label, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(4.dp)) {
                        Text(value, style = MotouringTextStyles.statValue)
                        Text(label.uppercase(), style = MotouringTextStyles.statLabel, color = Muted, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupBar(participants: List<RideParticipantState>) {
    val speaker = participants.firstOrNull { it.isSpeaking }
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Charcoal800).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            participants.take(4).forEachIndexed { i, p ->
                Box(
                    modifier = Modifier.offset(x = (i * -8).dp).size(26.dp).clip(CircleShape).background(colorFor(p.userId)),
                    contentAlignment = Alignment.Center,
                ) { Text(p.name.take(1), color = Color(0xFF100E0C), fontWeight = FontWeight.Bold) }
            }
            if (participants.size > 4) {
                Box(Modifier.offset(x = (4 * -8).dp).size(26.dp).clip(CircleShape).background(Charcoal700), contentAlignment = Alignment.Center) {
                    Text("+${participants.size - 4}", style = MotouringTextStyles.statLabel, color = Muted)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (speaker != null) {
            val t = rememberInfiniteTransition(label = "speak")
            val a by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "speakA")
            Box(Modifier.size(8.dp).clip(CircleShape).background(MotouringColors.speaking.copy(alpha = a)))
            Spacer(Modifier.width(6.dp))
            Text("${speaker.name} speaking", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

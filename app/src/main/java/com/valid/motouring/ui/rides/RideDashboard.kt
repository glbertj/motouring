package com.valid.motouring.ui.rides

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.data.model.activeLegDurationSeconds
import com.valid.motouring.data.model.avgSpeedKmh
import com.valid.motouring.data.model.toGoalMeters
import com.valid.motouring.simulation.gapsToAheadMeters
import com.valid.motouring.simulation.sortedByPackPosition
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
fun RideDashboard(
    session: RideSession,
    onSetRole: (String, RiderRole) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var showPack by remember { mutableStateOf(false) }
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
            Column(Modifier.weight(1f)) {
                StatPackToggle(showPack = showPack, onToggle = { showPack = it })
                Spacer(Modifier.height(8.dp))
                if (showPack) {
                    FormationList(session.participants, onSetRole)
                } else {
                    StatGrid(session)
                }
            }
        }
        if (!showPack) {
            Spacer(Modifier.height(12.dp))
            GroupBar(session.participants)
        }
    }
}

@Composable
private fun StatPackToggle(showPack: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Charcoal800).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ToggleCell("Stats", selected = !showPack, modifier = Modifier.weight(1f)) { onToggle(false) }
        ToggleCell("Pack", selected = showPack, modifier = Modifier.weight(1f)) { onToggle(true) }
    }
}

@Composable
private fun ToggleCell(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) Charcoal700 else ComposeColor.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MotouringColors.rider else Muted)
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

@Composable
private fun FormationList(participants: List<RideParticipantState>, onSetRole: (String, RiderRole) -> Unit) {
    val sorted = sortedByPackPosition(participants)
    val gaps = gapsToAheadMeters(sorted)
    Column(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Charcoal800).padding(8.dp)) {
        sorted.forEachIndexed { index, p ->
            FormationRow(participant = p, gapAheadMeters = gaps[index], onSetRole = onSetRole)
        }
    }
}

@Composable
private fun FormationRow(participant: RideParticipantState, gapAheadMeters: Double, onSetRole: (String, RiderRole) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { menuOpen = true }.padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(roleColor(participant)),
                contentAlignment = Alignment.Center,
            ) { Text(participant.name.take(1), color = Color(0xFF100E0C), fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(participant.name, style = MaterialTheme.typography.bodyMedium)
                if (gapAheadMeters > 0.0) {
                    Text("%.1f km back".format(gapAheadMeters / 1000.0), style = MotouringTextStyles.statLabel, color = Muted)
                }
            }
            if (participant.isSpeaking) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(MotouringColors.speaking))
                Spacer(Modifier.width(8.dp))
            }
            RoleBadge(participant)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Make Lead") }, onClick = { onSetRole(participant.userId, RiderRole.LEAD); menuOpen = false })
            DropdownMenuItem(text = { Text("Make Sweep") }, onClick = { onSetRole(participant.userId, RiderRole.SWEEP); menuOpen = false })
            DropdownMenuItem(text = { Text("Make Rider") }, onClick = { onSetRole(participant.userId, RiderRole.RIDER); menuOpen = false })
        }
    }
}

private fun roleColor(p: RideParticipantState): ComposeColor = when {
    p.hasFallenBehind -> MotouringColors.riderCoral
    p.role == RiderRole.LEAD -> MotouringColors.goal
    p.role == RiderRole.SWEEP -> MotouringColors.poiRest
    else -> MotouringColors.rider
}

@Composable
private fun RoleBadge(p: RideParticipantState) {
    val (label, color) = when (p.role) {
        RiderRole.LEAD -> "LEAD" to MotouringColors.goal
        RiderRole.SWEEP -> "SWEEP" to MotouringColors.poiRest
        RiderRole.RIDER -> return
    }
    Text(label, style = MotouringTextStyles.statLabel, color = color)
}

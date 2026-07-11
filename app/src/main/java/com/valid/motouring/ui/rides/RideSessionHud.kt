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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.activeLegDistanceMeters
import com.valid.motouring.data.model.activeLegDurationSeconds
import com.valid.motouring.ui.components.InstrumentRing
import com.valid.motouring.ui.components.StatBlock
import com.valid.motouring.ui.theme.AccentPrimary
import com.valid.motouring.ui.theme.MotouringTextStyles
import com.valid.motouring.ui.theme.Muted

@Composable
fun RideSessionHud(session: RideSession, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBlock(label = "Distance", value = "${"%.1f".format(session.distanceMeters / 1000.0)} km")
            StatBlock(label = "Speed", value = "${session.speedKmh.toInt()} km/h")
            StatBlock(label = "Duration", value = "${session.elapsedSeconds / 60} min")
        }
        Spacer(modifier = Modifier.height(12.dp))

        val goal = session.activeGoal
        if (session.mode == RideMode.GOAL && goal != null) {
            val remainingMeters = (goal.targetDistanceMeters - session.activeLegDistanceMeters()).coerceAtLeast(0.0)
            val progress = (session.activeLegDistanceMeters() / goal.targetDistanceMeters).toFloat().coerceIn(0f, 1f)
            val almostThere = progress >= 0.9f

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    if (almostThere) {
                        val infiniteTransition = rememberInfiniteTransition(label = "almostThereGlow")
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 0.6f,
                            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                            label = "glowAlpha",
                        )
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(AccentPrimary.copy(alpha = glowAlpha), AccentPrimary.copy(alpha = 0f)),
                                    ),
                                ),
                        )
                    }
                    InstrumentRing(progress = progress, size = 56.dp) {
                        Text(text = "${"%.1f".format(remainingMeters / 1000.0)} km", style = MotouringTextStyles.statLabel)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "→ ${goal.label}", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            val minutesSinceStop = session.activeLegDurationSeconds() / 60
            Text(
                text = "Endless — $minutesSinceStop min since last stop",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideSessionHudGoalPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RideSessionHud(
            session = com.valid.motouring.data.fake.FakeDataProvider.previewRideSessionWithGoal(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideSessionHudEndlessPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RideSessionHud(
            session = com.valid.motouring.data.fake.FakeDataProvider.previewRideSessionEndless(),
        )
    }
}

package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Leg
import com.valid.motouring.ui.components.MotouringCard

@Composable
fun GoalCelebrationOverlay(leg: Leg, modifier: Modifier = Modifier) {
    MotouringCard(modifier = modifier.fillMaxWidth().padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Goal reached!", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${"%.1f".format(leg.distanceMeters / 1000.0)} km in ${leg.durationSeconds / 60} min",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun GoalCelebrationOverlayPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        GoalCelebrationOverlay(
            leg = Leg(
                goal = com.valid.motouring.data.model.RideGoal(com.valid.motouring.data.model.GoalType.DISTANCE, "10 km", 10_000.0),
                distanceMeters = 10_200.0,
                durationSeconds = 1_260,
                avgSpeedKmh = 29.0,
                endReason = com.valid.motouring.data.model.LegEndReason.GOAL_REACHED,
            ),
        )
    }
}

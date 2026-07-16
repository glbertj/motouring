package com.valid.motouring.ui.rides

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StaggeredEntrance
import com.valid.motouring.ui.components.StatBlock
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted

@Composable
fun RideSummaryScreen(
    entry: RideHistoryEntry,
    earnedBadges: List<Badge>,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(text = "Ride Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = entry.routePreviewRes),
            contentDescription = entry.title,
            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = entry.title, style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatBlock(label = "Distance", value = "${"%.1f".format(entry.distanceMeters / 1000.0)} km")
            StatBlock(label = "Duration", value = "${entry.durationSeconds / 60} min")
            StatBlock(label = "Avg Speed", value = "${entry.avgSpeedKmh.toInt()} km/h")
        }

        entry.rideScore?.let { score ->
            SectionHeader(title = "Ride Score")
            MotouringCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${score.overall}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MotouringColors.goal)
                        Spacer(Modifier.padding(start = 6.dp))
                        Text("/ 100 · ${score.grade}", style = MaterialTheme.typography.titleMedium, color = Muted)
                    }
                    Spacer(Modifier.height(12.dp))
                    ScoreBar("Lean", score.lean)
                    ScoreBar("Smoothness", score.smoothness)
                    ScoreBar("Pace", score.pace)
                }
            }
            entry.segmentResult?.let { seg ->
                MotouringCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${seg.segmentName} · ${seg.timeSeconds / 60}:${(seg.timeSeconds % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.bodyMedium)
                        Text("#${seg.rank}", style = MaterialTheme.typography.titleMedium, color = MotouringColors.goal, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (earnedBadges.isNotEmpty()) {
            SectionHeader(title = "Your Badges")
            Row {
                earnedBadges.take(4).forEach { badge ->
                    BadgeChip(badge = badge, onClick = {}, modifier = Modifier.padding(end = 16.dp))
                }
            }
        }

        val visibleLegs = entry.legs.filter { it.goal != null || it.distanceMeters >= 500.0 }
        if (visibleLegs.isNotEmpty()) {
            SectionHeader(title = "Stops")
            visibleLegs.forEachIndexed { index, leg ->
                StaggeredEntrance(index = index) {
                    MotouringCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = leg.goal?.label ?: "Free ride", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = "${"%.1f".format(leg.distanceMeters / 1000.0)} km", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "${leg.durationSeconds / 60} min", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "${leg.avgSpeedKmh.toInt()} km/h", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}

@Composable
private fun ScoreBar(label: String, value: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
            Text("$value", style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            color = MotouringColors.goal,
            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideSummaryScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RideSummaryScreen(
            entry = com.valid.motouring.data.fake.FakeDataProvider.rideHistory.first(),
            earnedBadges = com.valid.motouring.data.fake.FakeDataProvider.badges.filter { it.isEarned },
            onDone = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideSummaryScreenWithStopsPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        val goalA = com.valid.motouring.data.model.RideGoal(com.valid.motouring.data.model.GoalType.DISTANCE, "10 km", 10_000.0)
        val goalB = com.valid.motouring.data.model.RideGoal(com.valid.motouring.data.model.GoalType.DESTINATION, "Warung Kopi Susu", 18_000.0)
        val legs = listOf(
            com.valid.motouring.data.model.Leg(goalA, 10_000.0, 1_200, 30.0, com.valid.motouring.data.model.LegEndReason.GOAL_REACHED),
            com.valid.motouring.data.model.Leg(goalB, 8_000.0, 960, 30.0, com.valid.motouring.data.model.LegEndReason.GOAL_REACHED),
            com.valid.motouring.data.model.Leg(null, 2_000.0, 300, 24.0, com.valid.motouring.data.model.LegEndReason.RIDE_ENDED),
        )
        RideSummaryScreen(
            entry = com.valid.motouring.data.fake.FakeDataProvider.rideHistory.first().copy(legs = legs),
            earnedBadges = com.valid.motouring.data.fake.FakeDataProvider.badges.filter { it.isEarned },
            onDone = {},
        )
    }
}

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StatBlock

@Composable
fun RideSummaryScreen(
    entry: RideHistoryEntry,
    earnedBadges: List<Badge>,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Ride Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = entry.routePreviewRes),
            contentDescription = entry.title,
            modifier = Modifier.fillMaxWidth().height(160.dp),
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

        if (earnedBadges.isNotEmpty()) {
            SectionHeader(title = "Your Badges")
            Row {
                earnedBadges.take(4).forEach { badge ->
                    BadgeChip(badge = badge, onClick = {}, modifier = Modifier.padding(end = 16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
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

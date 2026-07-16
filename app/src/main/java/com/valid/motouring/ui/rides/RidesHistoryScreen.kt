package com.valid.motouring.ui.rides

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.StatBlock
import com.valid.motouring.ui.components.StaggeredEntrance

@Composable
fun RidesHistoryScreen(history: List<RideHistoryEntry>, onSegmentsClick: () -> Unit = {}) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            MotouringCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), onClick = onSegmentsClick) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🏁 Segments & leaderboards", style = MaterialTheme.typography.titleMedium)
                    Text("›", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        itemsIndexed(history, key = { _, it -> it.id }) { index, entry ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(bottom = 12.dp)) {
                RideHistoryCard(entry)
            }
        }
    }
}

@Composable
private fun RideHistoryCard(entry: RideHistoryEntry) {
    MotouringCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Image(
                painter = painterResource(id = entry.routePreviewRes),
                contentDescription = entry.title,
                modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatBlock(label = "Distance", value = "${"%.1f".format(entry.distanceMeters / 1000.0)} km")
                StatBlock(label = "Duration", value = "${entry.durationSeconds / 60} min")
                StatBlock(label = "Avg Speed", value = "${entry.avgSpeedKmh.toInt()} km/h")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RidesHistoryScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RidesHistoryScreen(history = com.valid.motouring.data.fake.FakeDataProvider.rideHistory)
    }
}

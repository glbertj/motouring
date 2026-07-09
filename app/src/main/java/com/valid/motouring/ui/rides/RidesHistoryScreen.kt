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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.ui.components.StatBlock

@Composable
fun RidesHistoryScreen(history: List<RideHistoryEntry>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(history, key = { it.id }) { entry -> RideHistoryCard(entry) }
    }
}

@Composable
private fun RideHistoryCard(entry: RideHistoryEntry) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Image(
                painter = painterResource(id = entry.routePreviewRes),
                contentDescription = entry.title,
                modifier = Modifier.fillMaxWidth().height(100.dp),
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

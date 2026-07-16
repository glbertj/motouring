package com.valid.motouring.ui.scenic

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.theme.Muted

@Composable
fun ScenicRoutesScreen(viewModel: ScenicRoutesViewModel, onRouteClick: (String) -> Unit) {
    val routes by viewModel.routes.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Scenic Routes", style = MaterialTheme.typography.headlineMedium) }
        items(routes, key = { it.id }) { route ->
            MotouringCard(modifier = Modifier.fillMaxWidth(), onClick = { onRouteClick(route.id) }) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Image(
                        painter = painterResource(id = route.heroPhotoRes),
                        contentDescription = route.name,
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Text(route.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("${route.region} · ${"%.0f".format(route.distanceKm)} km · ${route.estimatedMinutes} min", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        route.vibe.forEach { VibeChip(it) }
                    }
                }
            }
        }
    }
}

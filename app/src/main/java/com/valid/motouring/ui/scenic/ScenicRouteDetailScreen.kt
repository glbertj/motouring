package com.valid.motouring.ui.scenic

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
import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.ui.components.map.MapCamera
import com.valid.motouring.ui.components.map.MapPolyline
import com.valid.motouring.ui.components.map.MotouringMap
import com.valid.motouring.ui.theme.Muted

@Composable
fun ScenicRouteDetailScreen(viewModel: ScenicRouteDetailViewModel, onRideRoute: () -> Unit) {
    val route by viewModel.route.collectAsState()
    val r = route ?: return

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Image(
            painter = painterResource(id = r.heroPhotoRes),
            contentDescription = r.name,
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.padding(20.dp)) {
            Text(r.name, style = MaterialTheme.typography.headlineMedium)
            Text("${r.region} · ${"%.0f".format(r.distanceKm)} km · ${r.estimatedMinutes} min", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                r.vibe.forEach { VibeChip(it) }
            }
            Spacer(Modifier.height(14.dp))
            Text(r.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            MotouringMap(
                cameraTarget = MapCamera(r.route.firstOrNull() ?: GeoPoint(0.0, 0.0), zoom = 11.0),
                markers = emptyList(),
                polyline = MapPolyline(r.route),
                onMarkerClick = {},
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRideRoute, modifier = Modifier.fillMaxWidth()) {
                Text("Ride this route")
            }
        }
    }
}

package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.ui.components.map.MapCamera
import com.valid.motouring.ui.components.map.MapMarker
import com.valid.motouring.ui.components.map.MapPolyline
import com.valid.motouring.ui.components.map.MarkerStyle
import com.valid.motouring.ui.components.map.MotouringMap
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.MotouringTextStyles
import com.valid.motouring.ui.theme.Muted

@Composable
fun RideSessionHud(session: RideSession, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        val markers = session.participants.mapIndexed { i, p ->
            MapMarker(
                id = p.userId,
                point = p.position,
                style = if (i == 0) MarkerStyle.SELF else MarkerStyle.BUDDY,
            )
        }
        MotouringMap(
            cameraTarget = MapCamera(session.participants.first().position, zoom = 13.0),
            markers = markers,
            polyline = MapPolyline(session.route),
            onMarkerClick = {},
            modifier = Modifier.matchParentSize(),
        )
        Text(
            "${session.speedKmh.toInt()}",
            style = MotouringTextStyles.statValueLarge,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
        Text(
            "KM/H",
            style = MotouringTextStyles.statLabel, color = Muted,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 8.dp),
        )
        session.activeGoal?.let {
            Text(
                "→ ${it.label}",
                style = MaterialTheme.typography.labelLarge, color = MotouringColors.poiRepair,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
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

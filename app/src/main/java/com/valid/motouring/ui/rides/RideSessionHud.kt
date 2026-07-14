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
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.ui.components.map.MapCamera
import com.valid.motouring.ui.components.map.MapMarker
import com.valid.motouring.ui.components.map.MapPolyline
import com.valid.motouring.ui.components.map.MarkerStyle
import com.valid.motouring.ui.components.map.MotouringMap
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.MotouringTextStyles
import com.valid.motouring.ui.theme.Muted

private fun RiderRole.markerStyle(hasFallenBehind: Boolean): MarkerStyle = when {
    hasFallenBehind -> MarkerStyle.BEHIND
    this == RiderRole.LEAD -> MarkerStyle.LEAD
    this == RiderRole.SWEEP -> MarkerStyle.SWEEP
    else -> MarkerStyle.RIDER
}

@Composable
fun RideSessionHud(session: RideSession, rallyPoi: PointOfInterest? = null, onSosFire: () -> Unit = {}, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        val riderMarkers = session.participants.mapIndexed { i, p ->
            MapMarker(
                id = p.userId,
                point = p.position,
                style = p.role.markerStyle(p.hasFallenBehind),
                isSelf = i == 0,
            )
        }
        val markers = if (rallyPoi != null) {
            riderMarkers + MapMarker("rally-${rallyPoi.id}", rallyPoi.location, MarkerStyle.POI_FUEL, selected = true)
        } else {
            riderMarkers
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
                style = MaterialTheme.typography.labelLarge, color = MotouringColors.goal,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            )
        }
        SosButton(
            onFire = onSosFire,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
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

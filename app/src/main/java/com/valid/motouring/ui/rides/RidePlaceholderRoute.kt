package com.valid.motouring.ui.rides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.ui.theme.AccentPrimary
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.Charcoal800

@Composable
fun RidePlaceholderRoute(
    route: List<GeoPoint>,
    markerPosition: GeoPoint,
    modifier: Modifier = Modifier,
) {
    val minLat = route.minOf { it.lat }
    val maxLat = route.maxOf { it.lat }
    val minLng = route.minOf { it.lng }
    val maxLng = route.maxOf { it.lng }

    fun GeoPoint.toOffset(width: Float, height: Float, padding: Float): Offset {
        val xFraction = if (maxLng == minLng) 0.5f else ((lng - minLng) / (maxLng - minLng)).toFloat()
        val yFraction = if (maxLat == minLat) 0.5f else ((maxLat - lat) / (maxLat - minLat)).toFloat()
        return Offset(xFraction * width + padding, yFraction * height + padding)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Charcoal800),
    ) {
        val padding = 24f
        val w = size.width - padding * 2
        val h = size.height - padding * 2
        val points = route.map { it.toOffset(w, h, padding) }
        for (i in 0 until points.size - 1) {
            drawLine(color = Charcoal600, start = points[i], end = points[i + 1], strokeWidth = 6f, cap = StrokeCap.Round)
        }
        drawCircle(color = AccentPrimary, radius = 10f, center = markerPosition.toOffset(w, h, padding))
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RidePlaceholderRoutePreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        val route = com.valid.motouring.data.fake.FakeDataProvider.sampleRoute
        RidePlaceholderRoute(route = route, markerPosition = route[2])
    }
}

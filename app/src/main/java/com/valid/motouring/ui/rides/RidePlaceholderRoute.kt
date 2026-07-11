package com.valid.motouring.ui.rides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.ui.theme.AccentPrimary
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.Charcoal800

data class FallbackMarker(val point: GeoPoint, val color: Color, val radiusPx: Float = 12f)

/** Hand-drawn map stand-in. Used directly, and as MotouringMap's preview/offline fallback. */
@Composable
fun RidePlaceholderRoute(
    route: List<GeoPoint>,
    markers: List<FallbackMarker>,
    modifier: Modifier = Modifier,
) {
    val allPoints = route + markers.map { it.point }
    if (allPoints.isEmpty()) return
    val minLat = allPoints.minOf { it.lat }
    val maxLat = allPoints.maxOf { it.lat }
    val minLng = allPoints.minOf { it.lng }
    val maxLng = allPoints.maxOf { it.lng }

    fun GeoPoint.toOffset(w: Float, h: Float, pad: Float): Offset {
        val x = if (maxLng == minLng) 0.5f else ((lng - minLng) / (maxLng - minLng)).toFloat()
        val y = if (maxLat == minLat) 0.5f else ((maxLat - lat) / (maxLat - minLat)).toFloat()
        return Offset(x * w + pad, y * h + pad)
    }

    Canvas(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(Charcoal800),
    ) {
        val pad = 24f
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val pts = route.map { it.toOffset(w, h, pad) }
        for (i in 0 until pts.size - 1) {
            drawLine(AccentPrimary, pts[i], pts[i + 1], strokeWidth = 6f, cap = StrokeCap.Round)
        }
        markers.forEach { m -> drawCircle(m.color, radius = m.radiusPx, center = m.point.toOffset(w, h, pad)) }
        if (route.isEmpty() && markers.size >= 2) {
            // no route: hint connectivity between markers
            val mp = markers.map { it.point.toOffset(w, h, pad) }
            for (i in 0 until mp.size - 1) drawLine(Charcoal600, mp[i], mp[i + 1], strokeWidth = 3f)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RidePlaceholderRoutePreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        val route = com.valid.motouring.data.fake.FakeDataProvider.sampleRoute
        RidePlaceholderRoute(
            route = route,
            markers = listOf(FallbackMarker(route[2], AccentPrimary)),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

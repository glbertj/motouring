package com.valid.motouring.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.ui.rides.FallbackMarker
import com.valid.motouring.ui.rides.RidePlaceholderRoute
import com.valid.motouring.ui.theme.MotouringColors
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

data class MapCamera(val target: GeoPoint, val zoom: Double = 12.0)
enum class MarkerStyle { SELF, BUDDY, POI_FUEL, POI_REPAIR, POI_REST }
data class MapMarker(val id: String, val point: GeoPoint, val style: MarkerStyle, val selected: Boolean = false)
data class MapPolyline(val points: List<GeoPoint>)

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
private const val ROUTE_SOURCE = "route-src"
private const val ROUTE_LAYER = "route-lyr"
private const val MARKER_SOURCE = "marker-src"
private const val MARKER_LAYER = "marker-lyr"

@Composable
fun MarkerStyle.color(): ComposeColor = when (this) {
    MarkerStyle.SELF -> MotouringColors.rider
    MarkerStyle.BUDDY -> MotouringColors.poiRest
    MarkerStyle.POI_FUEL -> MotouringColors.poiFuel
    MarkerStyle.POI_REPAIR -> MotouringColors.poiRepair
    MarkerStyle.POI_REST -> MotouringColors.poiRest
}

@Composable
fun MotouringMap(
    cameraTarget: MapCamera,
    markers: List<MapMarker>,
    polyline: MapPolyline?,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        // Preview / screenshot-test / offline fallback: hand-drawn Canvas.
        val fb = markers.map { FallbackMarker(it.point, it.style.color(), if (it.selected) 18f else 12f) }
        RidePlaceholderRoute(route = polyline?.points ?: emptyList(), markers = fb, modifier = modifier)
        return
    }

    val colorsArgb = MarkerStyle.entries.associate { it.name to it.color().toArgb() }
    val mapView = rememberMapViewWithLifecycle()

    AndroidView(factory = { mapView }, modifier = modifier) { mv ->
        mv.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                renderRoute(style, polyline)
                renderMarkers(style, markers, colorsArgb)
                map.setOnMarkerLayerClick(onMarkerClick)
            }
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(cameraTarget.target.lat, cameraTarget.target.lng))
                .zoom(cameraTarget.zoom).build()
        }
    }
}

private fun renderRoute(style: Style, polyline: MapPolyline?) {
    if (polyline == null || polyline.points.size < 2) return
    val line = LineString.fromLngLats(polyline.points.map { Point.fromLngLat(it.lng, it.lat) })
    style.addSource(GeoJsonSource(ROUTE_SOURCE, line))
    style.addLayer(
        LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
            PropertyFactory.lineColor(MotouringColors.poiRepair.toArgb()),
            PropertyFactory.lineWidth(4f),
        ),
    )
}

private fun renderMarkers(style: Style, markers: List<MapMarker>, colorsArgb: Map<String, Int>) {
    val features = markers.map { m ->
        Feature.fromGeometry(Point.fromLngLat(m.point.lng, m.point.lat)).apply {
            addStringProperty("id", m.id)
            addStringProperty("style", m.style.name)
            addBooleanProperty("selected", m.selected)
        }
    }
    style.addSource(GeoJsonSource(MARKER_SOURCE, FeatureCollection.fromFeatures(features)))
    // color per feature via a match expression on the "style" property
    style.addLayer(
        CircleLayer(MARKER_LAYER, MARKER_SOURCE).withProperties(
            PropertyFactory.circleColor(
                Expression.match(
                    Expression.get("style"),
                    Expression.color(colorsArgb.values.first()),
                    *matchStops(colorsArgb),
                ),
            ),
            PropertyFactory.circleRadius(
                Expression.switchCase(
                    Expression.get("selected"),
                    Expression.literal(11f),
                    Expression.literal(7f),
                ),
            ),
            PropertyFactory.circleStrokeColor(android.graphics.Color.parseColor("#100E0C")),
            PropertyFactory.circleStrokeWidth(2f),
        ),
    )
}

private fun matchStops(colorsArgb: Map<String, Int>) =
    colorsArgb.flatMap { (name, argb) ->
        listOf(Expression.literal(name), Expression.color(argb))
    }.toTypedArray()

private fun MapLibreMap.setOnMarkerLayerClick(onMarkerClick: (String) -> Unit) {
    addOnMapClickListener { latLng ->
        val screen = projection.toScreenLocation(latLng)
        val feats = queryRenderedFeatures(screen, MARKER_LAYER)
        val id = feats.firstOrNull()?.getStringProperty("id")
        if (id != null) { onMarkerClick(id); true } else false
    }
}

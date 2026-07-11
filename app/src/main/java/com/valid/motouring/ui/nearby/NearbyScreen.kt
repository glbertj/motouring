package com.valid.motouring.ui.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.PoiType
import com.valid.motouring.ui.components.map.MapCamera
import com.valid.motouring.ui.components.map.MapMarker
import com.valid.motouring.ui.components.map.MarkerStyle
import com.valid.motouring.ui.components.map.MotouringMap
import com.valid.motouring.ui.theme.Charcoal800
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.MotouringTextStyles
import com.valid.motouring.ui.theme.Muted

private fun PoiType.markerStyle() = when (this) {
    PoiType.GAS_STATION -> MarkerStyle.POI_FUEL
    PoiType.REPAIR_SHOP -> MarkerStyle.POI_REPAIR
    PoiType.REST_STOP -> MarkerStyle.POI_REST
}

private fun PoiType.color() = when (this) {
    PoiType.GAS_STATION -> MotouringColors.poiFuel
    PoiType.REPAIR_SHOP -> MotouringColors.poiRepair
    PoiType.REST_STOP -> MotouringColors.poiRest
}

private fun PoiType.emoji() = when (this) {
    PoiType.GAS_STATION -> "⛽"
    PoiType.REPAIR_SHOP -> "🔧"
    PoiType.REST_STOP -> "🍜"
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(viewModel: NearbyViewModel) {
    val state by viewModel.state.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val listState = rememberLazyListState()

    // recenter + reveal on selection
    LaunchedEffect(state.selectedId) {
        val idx = state.items.indexOfFirst { it.selected }
        if (idx >= 0) {
            scaffoldState.bottomSheetState.partialExpand()
            listState.animateScrollToItem(idx)
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 160.dp,
        sheetContainerColor = Charcoal800,
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    "${state.items.size} places nearby",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                LazyColumn(state = listState) {
                    items(state.items, key = { it.poi.id }) { np ->
                        PoiCard(np, onClick = { viewModel.select(np.poi.id) })
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val markers = state.items.map {
                MapMarker(it.poi.id, it.poi.location, it.poi.type.markerStyle(), selected = it.selected)
            }
            MotouringMap(
                cameraTarget = MapCamera(state.cameraTarget, zoom = 13.0),
                markers = markers,
                polyline = null,
                onMarkerClick = { id -> viewModel.select(id) },
                modifier = Modifier.matchParentSize(),
            )
            FilterRow(
                state.filter,
                onSelect = viewModel::setFilter,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(current: PoiFilter, onSelect: (PoiFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier) {
        listOf(
            PoiFilter.ALL to "All",
            PoiFilter.FUEL to "⛽ Fuel",
            PoiFilter.REPAIR to "🔧 Repair",
            PoiFilter.REST to "🍜 Food",
        ).forEach { (f, label) ->
            FilterChip(
                selected = current == f,
                onClick = { onSelect(f) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 6.dp),
            )
        }
    }
}

@Composable
private fun PoiCard(np: NearbyPoi, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(13.dp))
            .background(if (np.selected) np.poi.type.color().copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickableNoRipple(onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(np.poi.type.color().copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(np.poi.type.emoji())
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(np.poi.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Row {
                Text("★ %.1f".format(np.poi.rating), style = MotouringTextStyles.statLabel, color = MotouringColors.poiRest)
                Spacer(Modifier.width(8.dp))
                Text("%.1f km".format(np.distanceKm), style = MotouringTextStyles.statLabel, color = Muted)
            }
        }
    }
}

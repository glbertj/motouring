package com.valid.motouring.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StatBlock
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(viewModel: InsightsViewModel) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium)

        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBlock(label = "Distance", value = "${state.totals.distanceKm.roundToInt()} km")
            StatBlock(label = "Rides", value = state.totals.rideCount.toString())
            StatBlock(label = "Hours", value = "${state.totals.movingHours.roundToInt()}")
        }

        SectionHeader(title = "Weekly Distance")
        MotouringCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) { WeeklyDistanceChart(state.weekly) }
        }

        SectionHeader(title = "Personal Records")
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBlock(label = "Longest", value = "${state.records.longestRideKm.roundToInt()} km")
            StatBlock(label = "Fastest Avg", value = "${state.records.fastestAvgKmh.roundToInt()} km/h")
            StatBlock(label = "Best Score", value = state.records.bestScore.toString())
        }

        SectionHeader(title = "By Vehicle")
        MotouringCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) { VehicleSplitBar(state.split) }
        }

        SectionHeader(title = "Ride Score Trend")
        MotouringCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) { ScoreTrendSparkline(state.scoreTrend) }
        }
    }
}

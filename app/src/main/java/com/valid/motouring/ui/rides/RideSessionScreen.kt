package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Leg
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideSessionEvent
import kotlinx.coroutines.delay

@Composable
fun RideSessionScreen(
    viewModel: RideSessionViewModel,
    onEndRide: (String) -> Unit,
) {
    val session by viewModel.session.collectAsState()
    var celebrationLeg by remember { mutableStateOf<Leg?>(null) }
    var showChoiceSheet by remember { mutableStateOf(false) }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var showDriftToast by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RideSessionEvent.GoalReached -> celebrationLeg = event.leg
                RideSessionEvent.DriftedToEndless -> showDriftToast = true
            }
        }
    }

    LaunchedEffect(celebrationLeg) {
        if (celebrationLeg != null) {
            delay(2_500)
            showChoiceSheet = true
        }
    }

    LaunchedEffect(showChoiceSheet) {
        if (showChoiceSheet) {
            delay(5_000)
            if (showChoiceSheet) {
                showChoiceSheet = false
                celebrationLeg = null
                showUndoSnackbar = true
            }
        }
    }

    LaunchedEffect(showUndoSnackbar) {
        if (showUndoSnackbar) {
            delay(4_500)
            showUndoSnackbar = false
        }
    }

    LaunchedEffect(showDriftToast) {
        if (showDriftToast) {
            delay(3_000)
            showDriftToast = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            RideSessionHud(session = session)
            RidePlaceholderRoute(
                route = session.route,
                markerPosition = session.participants.first().position,
            )
            if (session.mode == RideMode.ENDLESS) {
                Button(onClick = { showChoiceSheet = true }) { Text("Set a goal") }
            }
            Button(onClick = { viewModel.simulateDrift() }) { Text("Simulate off-route") }
            Button(onClick = { onEndRide(viewModel.endRide()) }) { Text("End Ride") }
        }

        val leg = celebrationLeg
        if (leg != null && !showChoiceSheet) {
            GoalCelebrationOverlay(leg = leg, modifier = Modifier.align(Alignment.Center))
        }

        if (showChoiceSheet) {
            GoalChoiceSheet(
                presets = FakeDataProvider.goalPresets,
                onPickGoal = { goal ->
                    viewModel.pickGoal(goal)
                    showChoiceSheet = false
                    celebrationLeg = null
                },
                onGoEndless = {
                    showChoiceSheet = false
                    celebrationLeg = null
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showUndoSnackbar) {
            UndoGoalSnackbar(
                onPickGoalClick = {
                    showUndoSnackbar = false
                    showChoiceSheet = true
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showDriftToast) {
            DriftToast(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

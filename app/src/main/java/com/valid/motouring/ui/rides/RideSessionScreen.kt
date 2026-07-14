package com.valid.motouring.ui.rides

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.valid.motouring.data.model.GroupSignal
import com.valid.motouring.data.model.GroupSignalType
import com.valid.motouring.data.model.Leg
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideSessionEvent
import com.valid.motouring.data.model.SafetyAlert
import com.valid.motouring.data.model.SafetyAlertType
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
    var regroupMessage by remember { mutableStateOf<String?>(null) }
    var fuelSignal by remember { mutableStateOf<GroupSignal?>(null) }
    var rallyPoi by remember { mutableStateOf<PointOfInterest?>(null) }
    val activeAlert by viewModel.activeAlert.collectAsState()
    var crashCountdownActive by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RideSessionEvent.GoalReached -> celebrationLeg = event.leg
                RideSessionEvent.DriftedToEndless -> showDriftToast = true
                is RideSessionEvent.RiderFellBehind ->
                    regroupMessage = "${event.participant.name} fell behind — regrouping"
                is RideSessionEvent.GroupSignalRaised -> when (event.signal.type) {
                    GroupSignalType.REGROUP -> regroupMessage = "Regroup — wait for me"
                    GroupSignalType.FUEL -> {
                        fuelSignal = event.signal
                        rallyPoi = event.signal.rallyPoi
                    }
                }
                RideSessionEvent.HardStopDetected -> crashCountdownActive = true
                is RideSessionEvent.RiderInTrouble -> viewModel.raiseInTroubleAlert(event.participant)
            }
        }
    }

    LaunchedEffect(celebrationLeg) {
        if (celebrationLeg != null) {
            delay(2_500)
            showChoiceSheet = true
            showDriftToast = false
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

    LaunchedEffect(regroupMessage) {
        if (regroupMessage != null) { delay(4_000); regroupMessage = null }
    }
    LaunchedEffect(fuelSignal) {
        if (fuelSignal != null) { delay(6_000); fuelSignal = null }
    }
    LaunchedEffect(rallyPoi) {
        if (rallyPoi != null) { delay(10_000); rallyPoi = null }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            RideSessionHud(session = session, rallyPoi = rallyPoi, onSosFire = { viewModel.triggerSos() }, modifier = Modifier.weight(0.55f).fillMaxWidth())
            Column(modifier = Modifier.weight(0.45f).fillMaxWidth()) {
                RideDashboard(session = session, onSetRole = { userId, role -> viewModel.setRole(userId, role) })
                Spacer(Modifier.weight(1f))
                RideDebugControls(
                    showSetGoal = session.mode == RideMode.ENDLESS,
                    onSetGoal = { showChoiceSheet = true; showDriftToast = false },
                    onRegroup = { viewModel.broadcastRegroup() },
                    onFuel = { viewModel.callFuel() },
                    onForceBehind = { viewModel.forceSweepBehind() },
                    onCrash = { viewModel.simulateHardStop() },
                    onTrouble = { viewModel.simulateRiderInTrouble() },
                    onDrift = { viewModel.simulateDrift() },
                    onEnd = { onEndRide(viewModel.endRide()) },
                )
            }
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
                    showDriftToast = false
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showDriftToast) {
            DriftToast(modifier = Modifier.align(Alignment.BottomCenter))
        }

        regroupMessage?.let {
            RegroupBanner(message = it, modifier = Modifier.align(Alignment.BottomCenter))
        }
        fuelSignal?.let {
            FuelCallBanner(fromName = it.fromName, poiName = it.rallyPoi?.name, modifier = Modifier.align(Alignment.BottomCenter))
        }

        activeAlert?.let { alert ->
            when (alert.type) {
                SafetyAlertType.RIDER_IN_TROUBLE ->
                    RiderInTroubleCard(alert = alert, onResolve = { viewModel.resolveActiveAlert() }, modifier = Modifier.align(Alignment.BottomCenter))
                else ->
                    SosActiveBanner(alert = alert, onSafe = { viewModel.resolveActiveAlert() }, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        if (crashCountdownActive) {
            CrashCountdownOverlay(
                onOk = { crashCountdownActive = false },
                onSend = { crashCountdownActive = false; viewModel.confirmCrashAlert() },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun RideDebugControls(
    showSetGoal: Boolean,
    onSetGoal: () -> Unit,
    onRegroup: () -> Unit,
    onFuel: () -> Unit,
    onForceBehind: () -> Unit,
    onCrash: () -> Unit,
    onTrouble: () -> Unit,
    onDrift: () -> Unit,
    onEnd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onRegroup) { Text("Regroup") }
        TextButton(onClick = onFuel) { Text("Fuel") }
        if (showSetGoal) TextButton(onClick = onSetGoal) { Text("Goal") }
        TextButton(onClick = onForceBehind) { Text("Behind") }
        TextButton(onClick = onCrash) { Text("Crash") }
        TextButton(onClick = onTrouble) { Text("Trouble") }
        TextButton(onClick = onDrift) { Text("Off-route") }
        Button(onClick = onEnd) { Text("End") }
    }
}

package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Vehicle
import com.valid.motouring.data.model.VehicleType

@Composable
fun StartRideScreen(
    vehicles: List<Vehicle>,
    onInviteBuddiesClick: () -> Unit,
    onStartRide: (VehicleType, Boolean) -> Unit,
) {
    var isGroup by remember { mutableStateOf(true) }
    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Start a Ride", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !isGroup, onClick = { isGroup = false }, label = { Text("Solo") })
            FilterChip(selected = isGroup, onClick = { isGroup = true }, label = { Text("Group") })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Pick a vehicle", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        vehicles.forEach { vehicle ->
            FilterChip(
                selected = selectedVehicle?.id == vehicle.id,
                onClick = { selectedVehicle = vehicle },
                label = { Text("${vehicle.make} ${vehicle.model}") },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (isGroup) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onInviteBuddiesClick, modifier = Modifier.fillMaxWidth()) {
                Text("Invite Ride Buddies")
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))

        Button(
            onClick = { selectedVehicle?.let { onStartRide(it.type, isGroup) } },
            enabled = selectedVehicle != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start Ride")
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun StartRideScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        StartRideScreen(
            vehicles = com.valid.motouring.data.fake.FakeDataProvider.vehicles.filter { it.ownerId == "u-me" },
            onInviteBuddiesClick = {},
            onStartRide = { _, _ -> },
        )
    }
}

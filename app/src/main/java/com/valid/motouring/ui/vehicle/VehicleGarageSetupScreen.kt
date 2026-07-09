package com.valid.motouring.ui.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valid.motouring.data.model.VehicleType

@Composable
fun VehicleGarageSetupScreen(
    viewModel: VehicleGarageViewModel,
    onVehicleAdded: () -> Unit,
) {
    var selectedType by remember { mutableStateOf(VehicleType.MOTORCYCLE) }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    val yearValue = year.toIntOrNull()
    val canContinue = make.isNotBlank() && model.isNotBlank() && yearValue != null

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Add your first ride", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Register a motorcycle or car so we know what you're riding",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedType == VehicleType.MOTORCYCLE,
                onClick = { selectedType = VehicleType.MOTORCYCLE },
                label = { Text("Motorcycle") },
            )
            FilterChip(
                selected = selectedType == VehicleType.CAR,
                onClick = { selectedType = VehicleType.CAR },
                label = { Text("Car") },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = make,
            onValueChange = { make = it },
            label = { Text("Make (e.g. Yamaha)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model (e.g. MT-25)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
        )
        OutlinedTextField(
            value = year,
            onValueChange = { year = it.filter(Char::isDigit) },
            label = { Text("Year") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true,
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.addVehicle(selectedType, make, model, requireNotNull(yearValue))
                onVehicleAdded()
            },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
    }
}

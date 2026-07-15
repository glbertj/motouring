package com.valid.motouring.ui.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.ServiceStatus
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted

private fun ServiceStatus.color(): Color = when (this) {
    ServiceStatus.OK -> MotouringColors.statusOk
    ServiceStatus.DUE_SOON -> MotouringColors.statusDueSoon
    ServiceStatus.OVERDUE -> MotouringColors.statusOverdue
}

private fun ServiceStatus.label(): String = when (this) {
    ServiceStatus.OK -> "OK"
    ServiceStatus.DUE_SOON -> "Due soon"
    ServiceStatus.OVERDUE -> "Overdue"
}

@Composable
fun VehicleMaintenanceScreen(viewModel: VehicleMaintenanceViewModel) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        val vehicle = state.vehicle
        Text(
            text = vehicle?.let { "${it.year} ${it.make} ${it.model}" } ?: "Vehicle",
            style = MaterialTheme.typography.headlineMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable { editing = true },
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("${vehicle?.odometerKm ?: 0}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.padding(start = 6.dp))
            Text("km · tap to edit", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(
            text = if (state.dueCount > 0) "${state.dueCount} need attention" else "All up to date",
            color = if (state.dueCount > 0) MotouringColors.statusOverdue else MotouringColors.statusOk,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.items, key = { it.item.type }) { ui ->
                MotouringCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ui.item.type.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            StatusChip(ui.status)
                        }
                        Text(
                            "last ${ui.item.lastServicedKm} km · ${ui.kmSince} ago · every ${ui.item.intervalKm}",
                            style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 2.dp),
                        )
                        LinearProgressIndicator(
                            progress = { ui.progress },
                            color = ui.status.color(),
                            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 8.dp),
                        )
                        if (ui.status != ServiceStatus.OK) {
                            TextButton(onClick = { viewModel.markServiced(ui.item.type) }, modifier = Modifier.padding(top = 4.dp)) {
                                Text("Mark serviced")
                            }
                        }
                    }
                }
            }
        }
    }

    if (editing) {
        OdometerDialog(
            current = state.vehicle?.odometerKm ?: 0,
            onConfirm = { viewModel.setOdometer(it); editing = false },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun StatusChip(status: ServiceStatus) {
    Text(
        text = status.label().uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = status.color(),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(status.color().copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun OdometerDialog(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update odometer") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(7) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.toIntOrNull() ?: current) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

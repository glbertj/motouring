package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.MotouringCard

@Composable
fun UndoGoalSnackbar(onPickGoalClick: () -> Unit, modifier: Modifier = Modifier) {
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Went Endless — pick a goal?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onPickGoalClick) { Text("Pick a goal") }
        }
    }
}

@Composable
fun DriftToast(modifier: Modifier = Modifier) {
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Off route — tracking continues",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
fun RegroupBanner(message: String, modifier: Modifier = Modifier) {
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = com.valid.motouring.ui.theme.MotouringColors.riderCoral,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
fun FuelCallBanner(fromName: String, poiName: String?, modifier: Modifier = Modifier) {
    val text = if (poiName != null) "$fromName needs fuel — rally at $poiName" else "$fromName needs fuel — find a stop"
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = com.valid.motouring.ui.theme.MotouringColors.poiFuel,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun UndoGoalSnackbarPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        UndoGoalSnackbar(onPickGoalClick = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun DriftToastPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        DriftToast()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RegroupBannerPreview() {
    com.valid.motouring.ui.theme.MotouringTheme { RegroupBanner("Bagas fell behind — regrouping") }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun FuelCallBannerPreview() {
    com.valid.motouring.ui.theme.MotouringTheme { FuelCallBanner("Dinda", "Pertamina Sudirman") }
}

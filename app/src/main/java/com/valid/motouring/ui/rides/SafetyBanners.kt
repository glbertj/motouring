package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.SafetyAlert
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted

@Composable
fun SosActiveBanner(alert: SafetyAlert, onSafe: () -> Unit, modifier: Modifier = Modifier) {
    val to = if (alert.notifiedContactNames.isEmpty()) "your group" else alert.notifiedContactNames.joinToString(", ")
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("SOS active · sharing live location", color = MotouringColors.sos, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text("Sent to $to", color = Muted, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                alert.respondingContactName?.let {
                    Text("$it responding ✓", color = MotouringColors.speaking, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSafe) { Text("I'm safe now", color = MotouringColors.sos) }
            }
        }
    }
}

@Composable
fun RiderInTroubleCard(alert: SafetyAlert, onResolve: () -> Unit, modifier: Modifier = Modifier) {
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("${alert.fromName} may be in trouble", color = MotouringColors.sos, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text("Out of contact · way behind · stopped off-route", color = Muted, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {}) { Text("Locate") }
                TextButton(onClick = {}) { Text("Call") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onResolve) { Text("Dismiss", color = MotouringColors.sos) }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SosActiveBannerPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        SosActiveBanner(
            alert = com.valid.motouring.data.model.SafetyAlert(
                "p", com.valid.motouring.data.model.SafetyAlertType.SOS, "u-me", "Rafi",
                listOf("Dinda", "Bagas"), respondingContactName = "Dinda", startedAtSeconds = 0,
            ),
            onSafe = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RiderInTroubleCardPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RiderInTroubleCard(
            alert = com.valid.motouring.data.model.SafetyAlert(
                "p", com.valid.motouring.data.model.SafetyAlertType.RIDER_IN_TROUBLE, "u-3", "Bagas",
                emptyList(), startedAtSeconds = 0,
            ),
            onResolve = {},
        )
    }
}

package com.valid.motouring.ui.rides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valid.motouring.ui.theme.MotouringColors
import kotlinx.coroutines.delay

private const val COUNTDOWN_START = 15

@Composable
fun CrashCountdownOverlay(onOk: () -> Unit, onSend: () -> Unit, modifier: Modifier = Modifier) {
    var remaining by remember { mutableIntStateOf(COUNTDOWN_START) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
        onSend()
    }
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(32.dp),
        ) {
            Text("⚠ POSSIBLE CRASH", color = MotouringColors.sos, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text("Are you OK?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Auto-alerting your group & trusted contacts",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(28.dp))
            Text("$remaining", color = MotouringColors.sos, fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onOk,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            ) { Text("I'm OK", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSend) { Text("Send alert now", color = MotouringColors.sos) }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun CrashCountdownOverlayPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        CrashCountdownOverlay(onOk = {}, onSend = {})
    }
}

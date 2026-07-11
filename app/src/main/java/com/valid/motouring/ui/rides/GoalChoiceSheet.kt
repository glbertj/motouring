package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.valid.motouring.data.model.RideGoal
import com.valid.motouring.ui.components.MotouringCard

@Composable
fun GoalChoiceSheet(
    presets: List<RideGoal>,
    onPickGoal: (RideGoal) -> Unit,
    onGoEndless: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(presets.firstOrNull()) }
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Nice! Pick a new goal, or keep riding",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            presets.forEach { goal ->
                FilterChip(
                    selected = selected == goal,
                    onClick = { selected = goal },
                    label = { Text(goal.label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            OutlinedButton(onClick = onGoEndless, modifier = Modifier.padding(end = 8.dp)) {
                Text("Go Endless")
            }
            Button(onClick = { selected?.let(onPickGoal) }, enabled = selected != null) {
                Text("Pick Goal")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun GoalChoiceSheetPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        GoalChoiceSheet(
            presets = com.valid.motouring.data.fake.FakeDataProvider.goalPresets,
            onPickGoal = {},
            onGoEndless = {},
        )
    }
}

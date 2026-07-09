package com.valid.motouring.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.valid.motouring.ui.theme.MotouringTextStyles
import com.valid.motouring.ui.theme.MutedDim

@Composable
fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MotouringTextStyles.statValue, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label.uppercase(), style = MotouringTextStyles.statLabel, color = MutedDim)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun StatBlockPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        StatBlock(label = "Distance", value = "18.4 km")
    }
}

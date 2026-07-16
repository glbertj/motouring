package com.valid.motouring.ui.scenic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.ScenicVibe
import com.valid.motouring.ui.theme.MotouringColors

private fun ScenicVibe.color(): Color = when (this) {
    ScenicVibe.COASTAL -> MotouringColors.rider
    ScenicVibe.MOUNTAIN -> MotouringColors.riderPurple
    ScenicVibe.FOREST -> MotouringColors.poiFuel
    ScenicVibe.URBAN -> MotouringColors.poiRest
}

private fun ScenicVibe.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun VibeChip(vibe: ScenicVibe, modifier: Modifier = Modifier) {
    Text(
        text = vibe.label(),
        style = MaterialTheme.typography.labelSmall,
        color = vibe.color(),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(vibe.color().copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

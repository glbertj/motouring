package com.valid.motouring.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.MotouringColors

@Composable
fun BadgeChip(badge: Badge, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InstrumentRing(
            progress = if (badge.isEarned) 1f else 0f,
            size = 56.dp,
            strokeWidth = 2.5.dp,
            showTicks = false,
            showGlow = badge.isEarned,
        ) {
            Image(
                painter = painterResource(id = badge.iconRes),
                contentDescription = badge.title,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = badge.title,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BadgeChipPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        BadgeChip(badge = com.valid.motouring.data.fake.FakeDataProvider.badges.first(), onClick = {})
    }
}

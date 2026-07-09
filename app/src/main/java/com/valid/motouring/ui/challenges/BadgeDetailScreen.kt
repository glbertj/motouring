package com.valid.motouring.ui.challenges

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.ui.components.InstrumentRing

@Composable
fun BadgeDetailScreen(badge: Badge) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InstrumentRing(
            progress = if (badge.isEarned) 1f else 0f,
            size = 120.dp,
            strokeWidth = 5.dp,
            showGlow = badge.isEarned,
        ) {
            Image(
                painter = painterResource(id = badge.iconRes),
                contentDescription = badge.title,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = badge.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = badge.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Unlock criteria: ${badge.unlockCriteria}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (badge.isEarned) "Earned" else "Not yet earned",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BadgeDetailScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        BadgeDetailScreen(badge = com.valid.motouring.data.fake.FakeDataProvider.badges.first())
    }
}

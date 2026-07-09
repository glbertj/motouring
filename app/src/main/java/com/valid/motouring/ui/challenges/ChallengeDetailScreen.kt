package com.valid.motouring.ui.challenges

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Challenge

@Composable
fun ChallengeDetailScreen(challenge: Challenge) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        item {
            Text(text = challenge.title, style = MaterialTheme.typography.headlineMedium)
            Text(text = challenge.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (challenge.currentValue / challenge.goalValue).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${challenge.currentValue.toInt()} / ${challenge.goalValue.toInt()}",
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Leaderboard", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        itemsIndexed(challenge.leaderboard) { index, entry ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "#${index + 1}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(id = entry.avatarRes),
                    contentDescription = entry.name,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = entry.name, modifier = Modifier.weight(1f))
                Text(text = entry.progressValue.toInt().toString(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ChallengeDetailScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        ChallengeDetailScreen(challenge = com.valid.motouring.data.fake.FakeDataProvider.challenges.first())
    }
}

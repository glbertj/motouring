package com.valid.motouring.ui.challenges

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Challenge
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.InstrumentRing
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StaggeredEntrance
import com.valid.motouring.ui.theme.MotouringTextStyles

@Composable
fun ChallengesScreen(
    viewModel: ChallengesViewModel,
    onChallengeClick: (String) -> Unit,
    onSeeAllBadgesClick: () -> Unit,
    onBadgeClick: (String) -> Unit,
) {
    val challenges by viewModel.challenges.collectAsState()
    val badges by viewModel.badges.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { SectionHeader(title = "Active Challenges") }
        itemsIndexed(challenges, key = { _, it -> it.id }) { index, challenge ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(bottom = 12.dp)) {
                ChallengeRow(challenge = challenge, onClick = { onChallengeClick(challenge.id) })
            }
        }
        item {
            SectionHeader(title = "Badges", actionLabel = "See All", onActionClick = onSeeAllBadgesClick)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                badges.take(4).forEach { badge ->
                    BadgeChip(
                        badge = badge,
                        onClick = { onBadgeClick(badge.id) },
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeRow(challenge: Challenge, onClick: () -> Unit) {
    MotouringCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = challenge.title, style = MaterialTheme.typography.titleMedium)
                Text(text = challenge.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${challenge.currentValue.toInt()} / ${challenge.goalValue.toInt()}",
                    style = MotouringTextStyles.statLabel,
                )
            }
            InstrumentRing(
                progress = (challenge.currentValue / challenge.goalValue).toFloat(),
                size = 48.dp,
                showTicks = false,
            )
        }
    }
}

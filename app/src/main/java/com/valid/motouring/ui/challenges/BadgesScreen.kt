package com.valid.motouring.ui.challenges

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.ui.components.BadgeChip

@Composable
fun BadgesScreen(badges: List<Badge>, onBadgeClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(badges, key = { it.id }) { badge ->
            BadgeChip(
                badge = badge,
                onClick = { onBadgeClick(badge.id) },
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BadgesScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        BadgesScreen(badges = com.valid.motouring.data.fake.FakeDataProvider.badges, onBadgeClick = {})
    }
}

package com.valid.motouring.ui.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideBuddy

@Composable
fun InviteRideScreen(viewModel: InviteRideViewModel, onDone: () -> Unit) {
    val friends by viewModel.friends.collectAsState()
    val selectedUserIds by viewModel.selectedUserIds.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            item { Text(text = "Invite ride buddies", style = MaterialTheme.typography.headlineMedium) }
            items(friends, key = { it.user.id }) { buddy ->
                FriendSelectRow(
                    buddy = buddy,
                    isSelected = buddy.user.id in selectedUserIds,
                    onToggle = { viewModel.toggleSelected(buddy.user.id) },
                )
            }
        }
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text("Done (${selectedUserIds.size} invited)")
        }
    }
}

@Composable
private fun FriendSelectRow(buddy: RideBuddy, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = buddy.user.avatarRes),
                contentDescription = buddy.user.name,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = buddy.user.name, style = MaterialTheme.typography.titleMedium)
        }
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
    }
}

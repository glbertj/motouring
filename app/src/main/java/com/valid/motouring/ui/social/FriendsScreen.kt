package com.valid.motouring.ui.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
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
import com.valid.motouring.data.model.BuddyStatus
import com.valid.motouring.data.model.RideBuddy
import com.valid.motouring.ui.components.StaggeredEntrance

@Composable
fun FriendsScreen(viewModel: FriendsViewModel) {
    val buddies by viewModel.buddies.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(buddies, key = { _, it -> it.user.id }) { index, buddy ->
            StaggeredEntrance(index = index) {
                BuddyRow(
                    buddy = buddy,
                    onAccept = { viewModel.accept(buddy.user.id) },
                    onAdd = { viewModel.sendRequest(buddy.user.id) },
                )
            }
        }
    }
}

@Composable
private fun BuddyRow(buddy: RideBuddy, onAccept: () -> Unit, onAdd: () -> Unit) {
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
        when (buddy.status) {
            BuddyStatus.FRIEND -> Text(text = "Friend", style = MaterialTheme.typography.labelSmall)
            BuddyStatus.PENDING_SENT -> Text(text = "Requested", style = MaterialTheme.typography.labelSmall)
            BuddyStatus.PENDING_RECEIVED -> Button(onClick = onAccept) { Text("Accept") }
            BuddyStatus.NOT_CONNECTED -> Button(onClick = onAdd) { Text("Add") }
        }
    }
}

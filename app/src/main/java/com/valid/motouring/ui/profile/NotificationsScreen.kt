package com.valid.motouring.ui.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Notification

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(notifications, key = { it.id }) { notification ->
            NotificationRow(notification = notification, onClick = { viewModel.markRead(notification.id) })
        }
    }
}

@Composable
private fun NotificationRow(notification: Notification, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        onClick = onClick,
    ) {
        Text(
            text = notification.message,
            style = if (notification.isRead) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.titleMedium
            },
            modifier = Modifier.padding(12.dp),
        )
    }
}

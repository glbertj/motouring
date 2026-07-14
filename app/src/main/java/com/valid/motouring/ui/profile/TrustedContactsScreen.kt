package com.valid.motouring.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.Muted

@Composable
fun TrustedContactsScreen(viewModel: TrustedContactsViewModel) {
    val friends by viewModel.state.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Trusted Contacts", style = MaterialTheme.typography.headlineMedium)
        Text(
            "They get your SOS & crash alerts with your live location.",
            style = MaterialTheme.typography.bodySmall, color = Muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        friends.forEach { buddy ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(buddy.user.name, style = MaterialTheme.typography.bodyLarge)
                Switch(checked = buddy.isTrustedContact, onCheckedChange = { viewModel.toggle(buddy.user.id, it) })
            }
        }
    }
}

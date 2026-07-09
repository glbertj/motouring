package com.valid.motouring.ui.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.R
import com.valid.motouring.data.model.RideHistoryEntry

@Composable
fun CreatePostScreen(
    viewModel: PostViewModel,
    onPosted: () -> Unit,
) {
    var caption by remember { mutableStateOf("") }
    var attachedRide by remember { mutableStateOf<RideHistoryEntry?>(null) }
    var rideMenuExpanded by remember { mutableStateOf(false) }
    val rideHistory by viewModel.rideHistory.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "New Post", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_photo_placeholder),
            contentDescription = "Selected photo",
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)),
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text("Caption") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column {
            TextButton(onClick = { rideMenuExpanded = true }) {
                Text(attachedRide?.title ?: "Attach a ride (optional)")
            }
            DropdownMenu(expanded = rideMenuExpanded, onDismissRequest = { rideMenuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = { attachedRide = null; rideMenuExpanded = false },
                )
                rideHistory.forEach { ride ->
                    DropdownMenuItem(
                        text = { Text(ride.title) },
                        onClick = { attachedRide = ride; rideMenuExpanded = false },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))

        Button(
            onClick = {
                viewModel.createPost(caption, attachedRide?.id)
                onPosted()
            },
            enabled = caption.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Post")
        }
    }
}

package com.valid.motouring.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StatBlock
import com.valid.motouring.ui.components.StaggeredEntrance

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onFriendsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onVehicleClick: (String) -> Unit,
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val totalRides by viewModel.totalRides.collectAsState()
    val totalDistanceKm by viewModel.totalDistanceKm.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val dueCounts by viewModel.dueCounts.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = viewModel.currentUser.avatarRes),
                    contentDescription = viewModel.currentUser.name,
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.padding(start = 12.dp))
                Text(text = viewModel.currentUser.name, style = MaterialTheme.typography.headlineMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatBlock(label = "Rides", value = totalRides.toString())
                StatBlock(label = "Total Distance", value = "${"%.1f".format(totalDistanceKm)} km")
                StatBlock(label = "Badges", value = badges.count { it.isEarned }.toString())
            }

            SectionHeader(title = "My Garage")
        }
        itemsIndexed(vehicles) { index, vehicle ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(bottom = 8.dp)) {
                MotouringCard(modifier = Modifier.fillMaxWidth(), onClick = { onVehicleClick(vehicle.id) }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = vehicle.photoRes),
                            contentDescription = "${vehicle.make} ${vehicle.model}",
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.padding(start = 12.dp))
                        Text(text = "${vehicle.year} ${vehicle.make} ${vehicle.model}", modifier = Modifier.weight(1f))
                        val due = dueCounts[vehicle.id] ?: 0
                        if (due > 0) {
                            Text(
                                text = "$due due",
                                style = MaterialTheme.typography.labelSmall,
                                color = com.valid.motouring.ui.theme.MotouringColors.statusOverdue,
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                    .background(com.valid.motouring.ui.theme.MotouringColors.statusOverdue.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionHeader(title = "Badges")
            Row {
                badges.take(4).forEach { badge ->
                    BadgeChip(badge = badge, onClick = {}, modifier = Modifier.padding(end = 16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onInsightsClick) { Text("Insights") }
            TextButton(onClick = onFriendsClick) { Text("Ride Buddies") }
            TextButton(onClick = onNotificationsClick) { Text("Notifications") }
            TextButton(onClick = onEditProfileClick) { Text("Edit Profile") }
            TextButton(onClick = onSettingsClick) { Text("Settings") }
        }
    }
}

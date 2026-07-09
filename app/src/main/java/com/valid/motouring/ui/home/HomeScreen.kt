package com.valid.motouring.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.InstrumentRing
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.PostCard
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StaggeredEntrance
import com.valid.motouring.ui.theme.MotouringTextStyles

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartRideClick: () -> Unit,
    onPostClick: (String) -> Unit,
    onCreatePostClick: () -> Unit,
) {
    val posts by viewModel.posts.collectAsState()
    val featuredChallenge by viewModel.featuredChallenge.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Button(onClick = onStartRideClick, modifier = Modifier.fillMaxWidth()) {
                Text("Start Group Ride")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        featuredChallenge?.let { challenge ->
            item {
                MotouringCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(challenge.title.uppercase(), style = MotouringTextStyles.statLabel)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = challenge.currentValue.toInt().toString(),
                                    style = MotouringTextStyles.statValue,
                                )
                                Text(
                                    text = "/${challenge.goalValue.toInt()} km",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        InstrumentRing(
                            progress = (challenge.currentValue / challenge.goalValue).toFloat(),
                            size = 64.dp,
                        ) {
                            Text(
                                text = "${(challenge.currentValue / challenge.goalValue * 100).toInt()}%",
                                style = MotouringTextStyles.statValueLarge,
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionHeader(title = "Feed", actionLabel = "New Post", onActionClick = onCreatePostClick)
        }
        itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(bottom = 12.dp)) {
                PostCard(
                    post = post,
                    onLikeClick = { viewModel.toggleLike(post.id) },
                    onCardClick = { onPostClick(post.id) },
                )
            }
        }
    }
}

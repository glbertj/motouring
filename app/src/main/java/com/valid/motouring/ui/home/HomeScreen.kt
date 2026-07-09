package com.valid.motouring.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.PostCard
import com.valid.motouring.ui.components.SectionHeader

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
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(challenge.title, style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress = { (challenge.currentValue / challenge.goalValue).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            }
        }
        item {
            SectionHeader(title = "Feed", actionLabel = "New Post", onActionClick = onCreatePostClick)
        }
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onLikeClick = { viewModel.toggleLike(post.id) },
                onCardClick = { onPostClick(post.id) },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}

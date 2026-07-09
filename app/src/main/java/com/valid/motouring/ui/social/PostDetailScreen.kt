package com.valid.motouring.ui.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.PostCard

@Composable
fun PostDetailScreen(viewModel: PostViewModel) {
    val post by viewModel.post.collectAsState()
    val comments by viewModel.comments.collectAsState()
    var commentText by remember { mutableStateOf("") }

    val currentPost = post ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            item {
                PostCard(post = currentPost, onLikeClick = viewModel::toggleLike, onCardClick = {})
                Spacer(modifier = Modifier.height(16.dp))
                Text("Comments", style = MaterialTheme.typography.titleMedium)
            }
            items(comments, key = { it.id }) { comment ->
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    Image(
                        painter = painterResource(id = comment.authorAvatarRes),
                        contentDescription = comment.authorName,
                        modifier = Modifier.size(28.dp).clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = comment.authorName, style = MaterialTheme.typography.labelSmall)
                        Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment") },
            )
            IconButton(
                onClick = {
                    viewModel.addComment(commentText)
                    commentText = ""
                },
                enabled = commentText.isNotBlank(),
            ) {
                Icon(imageVector = Icons.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

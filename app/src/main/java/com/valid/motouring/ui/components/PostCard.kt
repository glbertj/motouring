package com.valid.motouring.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Post
import com.valid.motouring.ui.theme.MotouringColors

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MotouringCard(modifier = modifier.fillMaxWidth(), onClick = onCardClick) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = post.authorAvatarRes),
                    contentDescription = post.authorName,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = post.authorName, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (post.photoResList.isNotEmpty()) {
                Image(
                    painter = painterResource(id = post.photoResList.first()),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(text = post.caption, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (post.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.likedByMe) MotouringColors.liked else LocalContentColor.current,
                    )
                }
                Text(text = "${post.likeCount}")
                Spacer(modifier = Modifier.width(16.dp))
                Icon(imageVector = Icons.Filled.ChatBubbleOutline, contentDescription = "Comments")
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${post.commentIds.size}")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PostCardPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        PostCard(post = com.valid.motouring.data.fake.FakeDataProvider.posts.first(), onLikeClick = {}, onCardClick = {})
    }
}

package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Comment
import com.valid.motouring.data.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PostRepository {
    private val posts = MutableStateFlow(
        FakeDataProvider.posts.sortedByDescending { it.createdAtEpochSeconds }
    )
    private val comments = MutableStateFlow(FakeDataProvider.comments)

    fun observePosts(): StateFlow<List<Post>> = posts.asStateFlow()

    fun addPost(post: Post) {
        posts.value = (listOf(post) + posts.value)
            .sortedByDescending { it.createdAtEpochSeconds }
    }

    fun toggleLike(postId: String) {
        posts.value = posts.value.map {
            if (it.id == postId) {
                it.copy(
                    likedByMe = !it.likedByMe,
                    likeCount = if (it.likedByMe) it.likeCount - 1 else it.likeCount + 1,
                )
            } else it
        }
    }

    fun commentsFor(postId: String): List<Comment> =
        comments.value.filter { it.postId == postId }

    fun addComment(comment: Comment) {
        comments.value = comments.value + comment
        posts.value = posts.value.map {
            if (it.id == comment.postId) it.copy(commentIds = it.commentIds + comment.id) else it
        }
    }
}

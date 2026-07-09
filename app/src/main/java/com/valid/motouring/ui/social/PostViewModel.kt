package com.valid.motouring.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.R
import com.valid.motouring.data.model.Comment
import com.valid.motouring.data.model.Post
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.data.repository.PostRepository
import com.valid.motouring.data.repository.RideRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PostViewModel(
    private val postRepository: PostRepository,
    rideRepository: RideRepository,
    private val currentUserId: String,
    private val currentUserName: String,
    private val currentUserAvatarRes: Int,
    private val postId: String?,
) : ViewModel() {

    val post: StateFlow<Post?> = postRepository.observePosts()
        .map { posts -> posts.firstOrNull { it.id == postId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val comments: StateFlow<List<Comment>> = postRepository.observePosts()
        .map { postId?.let { postRepository.commentsFor(it) } ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rideHistory: StateFlow<List<RideHistoryEntry>> = rideRepository.observeHistory()

    fun toggleLike() {
        postId?.let { postRepository.toggleLike(it) }
    }

    fun addComment(text: String) {
        val targetPostId = postId ?: return
        if (text.isBlank()) return
        postRepository.addComment(
            Comment(
                id = "cm-${System.currentTimeMillis()}",
                postId = targetPostId,
                authorId = currentUserId,
                authorName = currentUserName,
                authorAvatarRes = currentUserAvatarRes,
                text = text,
                createdAtEpochSeconds = System.currentTimeMillis() / 1000,
            ),
        )
    }

    fun createPost(caption: String, attachedRideId: String?) {
        postRepository.addPost(
            Post(
                id = "post-${System.currentTimeMillis()}",
                authorId = currentUserId,
                authorName = currentUserName,
                authorAvatarRes = currentUserAvatarRes,
                photoResList = listOf(R.drawable.ic_photo_placeholder),
                caption = caption,
                attachedRideId = attachedRideId,
                likeCount = 0,
                likedByMe = false,
                commentIds = emptyList(),
                createdAtEpochSeconds = System.currentTimeMillis() / 1000,
            ),
        )
    }

    companion object {
        fun factory(
            postRepository: PostRepository,
            rideRepository: RideRepository,
            currentUserId: String,
            currentUserName: String,
            currentUserAvatarRes: Int,
            postId: String?,
        ) = viewModelFactory {
            initializer {
                PostViewModel(postRepository, rideRepository, currentUserId, currentUserName, currentUserAvatarRes, postId)
            }
        }
    }
}

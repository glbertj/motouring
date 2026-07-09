package com.valid.motouring.data.repository

import com.valid.motouring.data.model.Comment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostRepositoryTest {

    @Test
    fun `toggleLike increments count and flips likedByMe when not yet liked`() {
        val repo = PostRepository()
        val before = repo.observePosts().value.first { !it.likedByMe }

        repo.toggleLike(before.id)

        val after = repo.observePosts().value.first { it.id == before.id }
        assertTrue(after.likedByMe)
        assertEquals(before.likeCount + 1, after.likeCount)
    }

    @Test
    fun `toggleLike decrements count and flips likedByMe when already liked`() {
        val repo = PostRepository()
        val before = repo.observePosts().value.first { it.likedByMe }

        repo.toggleLike(before.id)

        val after = repo.observePosts().value.first { it.id == before.id }
        assertTrue(!after.likedByMe)
        assertEquals(before.likeCount - 1, after.likeCount)
    }

    @Test
    fun `addComment appends to commentsFor and links id back onto the post`() {
        val repo = PostRepository()
        val targetPost = repo.observePosts().value.first()
        val comment = Comment(
            id = "cm-new",
            postId = targetPost.id,
            authorId = "u-me",
            authorName = "Rafi",
            authorAvatarRes = 0,
            text = "Great ride!",
            createdAtEpochSeconds = 1_753_000_000,
        )

        repo.addComment(comment)

        assertTrue(repo.commentsFor(targetPost.id).any { it.id == "cm-new" })
        val updatedPost = repo.observePosts().value.first { it.id == targetPost.id }
        assertTrue(updatedPost.commentIds.contains("cm-new"))
    }
}

package com.example.socialmediaproject.dataclass

import com.google.firebase.firestore.Exclude

data class Comment(val id: String = "",
                   val content: String = "",
                   val userId: String = "",
                   val timestamp: Long = System.currentTimeMillis(),
                   val parentId: String? = null,
                   val postId: String = "",
                   val likes: List<String> = listOf(),
                    @get:Exclude
                    var replies: MutableList<Comment> = mutableListOf())

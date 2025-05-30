package com.example.socialmediaproject.dataclass

import com.google.firebase.firestore.Exclude

data class Comment(val id: String = "",
                   var content: String = "",
                   val userId: String = "",
                   val timestamp: Long = System.currentTimeMillis(),
                   val parentId: String? = null,
                   val postId: String = "",
                   var likes: List<String> = listOf(),
                   var username: String ="",
                   var avatarurl: String = "",
                   val mentionedUserIds: List<String> = emptyList(),
                   val notifiedUserIds: List<String> = emptyList(),
                   @get:Exclude
                    var replies: MutableList<Comment> = mutableListOf())

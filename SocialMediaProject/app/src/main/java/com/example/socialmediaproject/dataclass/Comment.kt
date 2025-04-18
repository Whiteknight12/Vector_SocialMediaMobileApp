package com.example.socialmediaproject.dataclass

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

data class Comment(val id: String = "",
                   val content: String = "",
                   val userId: String = "",
                   val timestamp: Long = System.currentTimeMillis(),
                   val parentId: String? = null,
                   val postId: String = "",
                   val likes: List<String> = listOf(),
                   var username: String ="",
                   var avatarurl: String = "",
                   val mentionedUserIds: List<String> = emptyList(),
                   val notifiedUserIds: List<String> = emptyList(),
                   @get:Exclude
                    var replies: MutableList<Comment> = mutableListOf())

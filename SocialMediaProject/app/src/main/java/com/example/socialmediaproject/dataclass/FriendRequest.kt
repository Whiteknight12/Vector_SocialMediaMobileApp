package com.example.socialmediaproject.dataclass

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FriendRequest(val senderId: String="",
    val receiverId: String="",
    val status: String="pending",
    @ServerTimestamp val timestamp: Date?=null)

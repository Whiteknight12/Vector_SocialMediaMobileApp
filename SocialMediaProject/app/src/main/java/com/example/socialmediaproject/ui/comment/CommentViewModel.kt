package com.example.socialmediaproject.ui.comment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaproject.dataclass.Comment
import com.example.socialmediaproject.service.OneSignalHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class CommentViewModel : ViewModel() {
    private val auth: FirebaseAuth=FirebaseAuth.getInstance()
    private val db: FirebaseFirestore =FirebaseFirestore.getInstance()

    fun postComment(content: String, parentId: String? = null, postId: String) {
        viewModelScope.launch {
            val commentId = db.collection("comments").document().id
            val comment = Comment(
                id = commentId,
                content = content,
                userId = auth.currentUser?.uid ?: "",
                parentId = parentId,
                postId = postId
            )
            db.collection("comments")
            .document(commentId)
            .set(comment)
            .addOnSuccessListener {
                handleMentions(content)
            }
            .addOnFailureListener {

            }
        }
    }

    fun getComments(onResult: (List<Comment>) -> Unit) {
        db.collection("comments")
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .addSnapshotListener { snapshots, error ->
            if (error != null || snapshots == null) return@addSnapshotListener
            val comments = snapshots.toObjects(Comment::class.java)
            val userIds = comments.map { it.userId }.toSet().filter { it.isNotBlank() }
            val batches = userIds.chunked(10)
            val userMap = mutableMapOf<String, Pair<String, String>>()
            var completedBatches = 0
            batches.forEach { batch ->
                db.collection("Users")
                .whereIn("userid", batch)
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        val id = doc.getString("userid") ?: return@forEach
                        val username = doc.getString("name") ?: "Unknown"
                        val avatarurl = doc.getString("avatarurl") ?: ""
                        userMap[id] = Pair(username, avatarurl)
                    }
                    completedBatches++
                    if (completedBatches == batches.size) {
                        comments.forEach { comment ->
                            userMap[comment.userId]?.let { (username, avatar) ->
                                comment.username = username
                                comment.avatarurl = avatar
                            }
                        }
                        onResult(comments)
                    }
                }
                .addOnFailureListener {
                    completedBatches++
                    if (completedBatches == batches.size) {
                        onResult(comments)
                    }
                }
            }
        }
    }

    fun toggleLikeComment(commentId: String, userId: String) {
        val commentRef = db.collection("comments").document(commentId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val currentLikes = snapshot.get("likes") as? List<String> ?: listOf()
            val updatedLikes = if (currentLikes.contains(userId)) {
                currentLikes - userId
            } else {
                currentLikes + userId
            }
            transaction.update(commentRef, "likes", updatedLikes)
        }.addOnSuccessListener {
            // Success
        }.addOnFailureListener {
            // Fail
        }
    }

    private fun handleMentions(content: String) {
        val pattern = Pattern.compile("@(\\w+)")
        val matcher = pattern.matcher(content)
        val mentionedUsernames = mutableSetOf<String>()
        while (matcher.find()) {
            mentionedUsernames.add(matcher.group(1))
        }
        if (mentionedUsernames.isEmpty()) return
        db.collection("Users")
        .whereIn("username", mentionedUsernames.toList())
        .get()
        .addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                val userId = doc.getString("userid") ?: continue
                val playerId = doc.getString("playerId") ?: continue
                val username = doc.getString("username") ?: "ai đó"
                OneSignalHelper.sendMentionNotification(playerId, username)
            }
        }
    }
}
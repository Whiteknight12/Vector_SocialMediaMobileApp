package com.example.socialmediaproject.ui.search

import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.socialmediaproject.dataclass.FriendRecommendation
import com.example.socialmediaproject.dataclass.FriendRequest
import com.example.socialmediaproject.dataclass.RequestStatus
import com.example.socialmediaproject.dataclass.User
import com.example.socialmediaproject.service.NotificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val context=application.applicationContext
    private val db: FirebaseFirestore=FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth=FirebaseAuth.getInstance()
    private val _recommendations= MutableStateFlow<List<FriendRecommendation>>(emptyList())
    private val _sentRequests=MutableStateFlow<List<FriendRecommendation>>(emptyList())
    private val _receivedRequests= MutableStateFlow<List<FriendRecommendation>>(emptyList())
    val recommendations: StateFlow<List<FriendRecommendation>> = _recommendations
    val sentRequests: StateFlow<List<FriendRecommendation>> = _sentRequests
    val receivedRequests: StateFlow<List<FriendRecommendation>> = _receivedRequests
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    private val _incomingRequestCount = MutableLiveData<Int>(0)
    val incomingRequestCount: LiveData<Int> = _incomingRequestCount
    private val sentRequestsStatus = mutableMapOf<String, RequestStatus>()
    private val _friendRequestCount = MutableLiveData<Int>()
    val friendRequestCount: LiveData<Int> = _friendRequestCount
    private var lastRequestCount=0

    init {
        listenForIncomingFriendRequests()
    }

    fun fetchRecommendations() {
        viewModelScope.launch {
            _isLoading.value=true
            _errorMessage.value=null
            val currentUserId=auth.currentUser?.uid ?: return@launch
            try {
                val currentUserDoc = db.collection("Users").document(currentUserId).get().await()
                val currentUser = currentUserDoc.toObject<User>()
                val friendsIds = currentUser?.friends ?: emptyList()
                val incomingRequests = db.collection("friend_requests")
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("status", "pending")
                    .get().await()
                val incomingRequesterIds=incomingRequests.documents.mapNotNull { it.getString("senderId") }
                val sentPendingRequests = db.collection("friend_requests")
                    .whereEqualTo("senderId", currentUserId)
                    .whereEqualTo("status", "pending")
                    .get().await()
                val sentPendingReceiverIds = sentPendingRequests.documents.mapNotNull { it.getString("receiverId") }
                val excludeIds = mutableListOf(currentUserId)
                excludeIds.addAll(friendsIds)
                excludeIds.addAll(incomingRequesterIds)
                excludeIds.addAll(sentPendingReceiverIds)
                val excludeChunks = excludeIds.distinct().chunked(30)
                val potentialFriends = mutableListOf<User>()
                for (chunk in excludeChunks) {
                    val usersSnapshot = db.collection("Users")
                        .whereNotIn("userid", chunk)
                        .limit(50)
                        .get()
                        .await()
                    for(doc in usersSnapshot.documents) {
                        val user = doc.toObject<User>()
                        if (user != null && user.userid !in excludeIds) {
                            potentialFriends.add(user)
                        }
                    }
                    if (potentialFriends.isNotEmpty()) break
                }
                _recommendations.value = potentialFriends.map { user ->
                    val mutualCount = currentUser?.friends?.intersect(user.friends ?: emptyList())?.size ?: 0
                    FriendRecommendation(
                        userId = user.userid,
                        name = user.name,
                        avatarurl = user.avatarurl,
                        mutualFriendsCount = mutualCount,
                        requestStatus = sentRequestsStatus[user.userid] ?: RequestStatus.NONE
                    )
                }
            }
            catch(e: Exception) {
                Log.e("ERROR FETCH RECOMMENDATION", e.toString())
            }
            finally {
                _isLoading.value=false
            }
        }
    }

    fun fetchFriendRequest() {
        viewModelScope.launch {
            val userid=auth.currentUser?.uid ?: ""
            var requests: Int=0
            db.collection("friend_requests").whereEqualTo("receiverId", userid).get().addOnSuccessListener {
                documents->if (documents!=null) {
                val requests = documents.size()
                _friendRequestCount.value = requests
                }
                _friendRequestCount.value = requests
            }
            .addOnFailureListener {
                _friendRequestCount.value = 0
            }
        }
    }

    fun sendFriendRequest(recommendation: FriendRecommendation) {
        viewModelScope.launch {
            val senderId=auth.currentUser?.uid ?: return@launch
            val receiverId=recommendation.userId
            updateRecommendationStatus(receiverId, RequestStatus.SENDING)
            sentRequestsStatus[receiverId] = RequestStatus.SENDING
            try {
                val requestId = "${senderId}_${receiverId}"
                val requestDocRef = db.collection("friend_requests").document(requestId)
                val existingRequest = requestDocRef.get().await()
                if (existingRequest.exists() && existingRequest.getString("status") != "rejected") {
                    updateRecommendationStatus(receiverId, RequestStatus.SENT)
                    sentRequestsStatus[receiverId] = RequestStatus.SENT
                    return@launch
                }
                val newRequest = FriendRequest(
                    senderId = senderId,
                    receiverId = receiverId,
                    status = "pending",
                    notified = false,
                    timestamp = Date()
                )
                requestDocRef.set(newRequest).await()
                updateRecommendationStatus(receiverId, RequestStatus.SENT)
                sentRequestsStatus[receiverId] = RequestStatus.SENT
            }
            catch(e: Exception) {
                updateRecommendationStatus(receiverId, RequestStatus.ERROR)
                sentRequestsStatus.remove(receiverId)
                Log.e("ERROR SEND FRIEND REQUEST", e.toString())
            }
        }
    }

    private fun updateRecommendationStatus(userId: String, status: RequestStatus) {
        val currentList = _recommendations.value.toMutableList()
        val index = currentList.indexOfFirst { it.userId == userId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(requestStatus = status)
            _recommendations.value = currentList
        }
    }

    private fun listenForIncomingFriendRequests() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("friend_requests")
        .whereEqualTo("receiverId", currentUserId)
        .whereEqualTo("status", "pending")
        .whereEqualTo("notified", false)
        .addSnapshotListener { snapshots, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            val count = snapshots?.size() ?: 0
            if (count != lastRequestCount) {
                _incomingRequestCount.postValue(count)
                if (count > 0) sendNotificationService("Bạn có ${count} lời mời kết bạn mới!")
                val batch = db.batch()
                snapshots?.documents?.forEach { doc ->
                    batch.update(doc.reference, "notified", true)
                }
                batch.commit()
            }
            lastRequestCount = count
        }
    }

    fun acceptFriendRequest(senderId: String, callback: (Boolean) -> Unit) {
        val receiverId = auth.currentUser?.uid ?: run {
            callback(false)
            return
        }
        viewModelScope.launch {
            try {
                val requestDocRef = db.collection("friend_requests").document("${senderId}_${receiverId}")
                val userRef = db.collection("Users").document(receiverId)
                val friendRef = db.collection("Users").document(senderId)
                db.runBatch { batch ->
                    batch.update(friendRef, "friends", FieldValue.arrayUnion(receiverId))
                    batch.update(userRef, "friends", FieldValue.arrayUnion(senderId))
                    batch.delete(requestDocRef)
                }.await()
                _incomingRequestCount.postValue((_incomingRequestCount.value ?: 0) - 1)
                callback(true)
            } catch (e: Exception) {
                Log.e("FriendRequest", "Error accepting friend request from $senderId for $receiverId", e)
                callback(false)
            }
        }
    }

    fun rejectFriendRequest(senderId: String, callback: (Boolean) -> Unit) {
        val receiverId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val requestDocRef = db.collection("friend_requests").document("${senderId}_${receiverId}")
                val requestSnapshot = requestDocRef.get().await()
                if (!requestSnapshot.exists()) {
                    callback(false)
                    return@launch
                }
                updateRecommendationStatus(receiverId, RequestStatus.NONE)
                sentRequestsStatus[receiverId] = RequestStatus.NONE
                requestDocRef.delete().await()
                _incomingRequestCount.postValue((_incomingRequestCount.value ?: 0) - 1)
                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun sendNotificationService(content: String) {
        val intent = Intent(context, NotificationService::class.java).apply {
            action = NotificationService.ACTION.START.toString()
            putExtra("content", content)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun resendFriendRequest(receiverId: String, callback: (Boolean)->Unit) {
        val senderId=auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val requestDocRef = db.collection("friend_requests").document("${senderId}_${receiverId}")
                val requestSnapshot = requestDocRef.get().await()
                updateRecommendationStatus(receiverId, RequestStatus.NONE)
                sentRequestsStatus[receiverId] = RequestStatus.NONE
                if (!requestSnapshot.exists()) {
                    callback(false)
                    return@launch
                }
                requestDocRef.delete().await()
                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    fun fetchSentRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val currentUserId = auth.currentUser?.uid ?: return@launch
            try {
                val sentPendingRequests = db.collection("friend_requests")
                    .whereEqualTo("senderId", currentUserId)
                    .whereEqualTo("status", "pending")
                    .get().await()
                val sentPendingReceiverIds = sentPendingRequests.documents.mapNotNull { it.getString("receiverId") }
                val sentRequestsUsers = mutableListOf<User>()
                if (sentPendingReceiverIds.isNotEmpty()) {
                    val usersSnapshot = db.collection("Users")
                        .whereIn("userid", sentPendingReceiverIds)
                        .get()
                        .await()

                    for (doc in usersSnapshot.documents) {
                        val user = doc.toObject<User>()
                        if (user != null) {
                            sentRequestsUsers.add(user)
                        }
                    }
                }

                _sentRequests.value = sentRequestsUsers.map { user ->
                    FriendRecommendation(
                        userId = user.userid,
                        name = user.name,
                        avatarurl = user.avatarurl,
                        mutualFriendsCount = 0,
                        requestStatus = RequestStatus.SENT
                    )
                }
            } catch (e: Exception) {
                Log.e("ERROR FETCH SENT REQUESTS", e.toString())
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchReceivedFriendRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val currentUserId = auth.currentUser?.uid ?: return@launch
            try {
                val receivedPendingRequests = db.collection("friend_requests")
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("status", "pending")
                    .get().await()
                val receivedPendingSenderIds = receivedPendingRequests.documents.mapNotNull { it.getString("senderId") }
                val receivedRequestsUsers = mutableListOf<User>()
                if (receivedPendingSenderIds.isNotEmpty()) {
                    val usersSnapshot = db.collection("Users")
                        .whereIn("userid", receivedPendingSenderIds)
                        .get()
                        .await()
                    for (doc in usersSnapshot.documents) {
                        val user = doc.toObject(User::class.java)
                        if (user != null) {
                            receivedRequestsUsers.add(user)
                        } else {
                            Log.w("FETCH_RECEIVED", "Failed to parse user document: ${doc.id}")
                        }
                    }
                }
                _receivedRequests.value = receivedRequestsUsers.map { user ->
                    FriendRecommendation(
                        userId = user.userid,
                        name = user.name,
                        avatarurl = user.avatarurl,
                        mutualFriendsCount = 0,
                        requestStatus = RequestStatus.SENT
                    )
                }
                Log.d("FETCH_RECEIVED", "Fetched ${receivedRequestsUsers.size} received requests.")
            } catch (e: Exception) {
                Log.e("FETCH_RECEIVED_ERROR", "Error fetching received friend requests", e)
                _errorMessage.value = "Failed to load received requests: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
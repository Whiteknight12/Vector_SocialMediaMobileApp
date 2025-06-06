package com.example.socialmediaproject.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Base64
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException

class PostingService : Service() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val API_KEY = "b5a914cc1aedaa51a1a0a5a4db8ed3ff"
    private val uploadedImage = arrayListOf<String>()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        var isposting=false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isposting =true
        val postContent = intent?.getStringExtra("post_content") ?: ""
        val imageList = intent?.getParcelableArrayListExtra<Uri>("image_list") ?: arrayListOf()
        val privacy = intent?.getStringExtra("privacy") ?: "Công khai"
        notifyStatus(NotificationService.ACTION.START, "Đang đăng bài")
        uploadAllImages(imageList) {
            UploadPost(postContent, privacy)
        }
        return START_NOT_STICKY
    }

    private fun uploadAllImages(imageList: List<Uri>, callback: () -> Unit) {
        var uploadedCount = 0
        if (imageList.isEmpty()) {
            callback()
            return
        }
        for (uri in imageList) {
            uploadImageToImgbb(uri) { imageUrl ->
                if (imageUrl != null) {
                    uploadedImage.add(imageUrl)
                }
                uploadedCount++
                if (uploadedCount == imageList.size) {
                    callback()
                }
            }
        }
    }

    private fun uploadImageToImgbb(imageUri: Uri, callback: (String?) -> Unit) {
        Thread {
            try {
                val base64Image = uriToBase64(imageUri) ?: return@Thread callback(null)
                val client = OkHttpClient()
                val requestBody = FormBody.Builder()
                    .add("key", API_KEY)
                    .add("image", base64Image)
                    .build()
                val request = Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(requestBody)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        callback(null)
                        return@use
                    }
                    val jsonResponse = JSONObject(response.body!!.string())
                    val imageUrl = jsonResponse.getJSONObject("data").getString("url")
                    callback(imageUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    private fun UploadPost(content: String, privacy: String) {
        getCategories { categories ->
            if (categories.isEmpty()) {
                notifyStatus(NotificationService.ACTION.UPDATE, "Lỗi trong quá trình phân tích!")
                notifyStatus(NotificationService.ACTION.STOP, "")
                isposting = false
                stopSelf()
                return@getCategories
            }
            serviceScope.launch {
                try {
                    notifyStatus(NotificationService.ACTION.UPDATE, "Đang phân tích nội dung...")
                    val response = AIService.classifyPost(content, categories) ?: ""
                    val categoryList = response.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val userid = auth.currentUser?.uid
                    val post = hashMapOf(
                        "userid" to userid,
                        "imageurl" to uploadedImage,
                        "content" to content,
                        "timestamp" to System.currentTimeMillis(),
                        "privacy" to privacy,
                        "category" to categoryList
                    )
                    categoryList.forEach { categoryName ->
                        db.collection("Categories")
                        .whereEqualTo("name", categoryName)
                        .get()
                        .addOnSuccessListener { result ->
                            if (result.isEmpty) {
                                val newCategory = hashMapOf(
                                    "name" to categoryName
                                )
                                db.collection("Categories")
                                .add(newCategory)
                                .addOnSuccessListener {

                                }
                            }
                        }
                    }
                    db.collection("Posts").add(post)
                        .addOnSuccessListener { docRef ->
                            val postId = docRef.id
                            savePostStatsToRealtimeDatabase(postId) {
                                notifyStatus(NotificationService.ACTION.UPDATE, "Đăng bài thành công!")
                                Handler(Looper.getMainLooper()).postDelayed({
                                    notifyStatus(NotificationService.ACTION.STOP, "")
                                }, 1500)
                                isposting = false
                                stopSelf()
                            }
                        }
                        .addOnFailureListener {
                            notifyStatus(NotificationService.ACTION.UPDATE, "Đăng bài thất bại!")
                            Handler(Looper.getMainLooper()).postDelayed({
                                notifyStatus(NotificationService.ACTION.STOP, "")
                            }, 1500)
                            isposting = false
                            stopSelf()
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    notifyStatus(NotificationService.ACTION.UPDATE, "Lỗi khi xử lý bài đăng!")
                    Handler(Looper.getMainLooper()).postDelayed({
                        notifyStatus(NotificationService.ACTION.STOP, "")
                    }, 1500)
                    isposting = false
                    stopSelf()
                }
            }
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getCategories(callback: (List<String>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Categories").get().addOnSuccessListener { documents ->
            val interests = mutableListOf<String>()
            for (document in documents) {
                document.getString("name")?.let { interests.add(it) }
            }
            callback(interests)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    private fun savePostStatsToRealtimeDatabase(postId: String, callback: () -> Unit) {
        val db=Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val databaseRef = db.getReference("PostStats").child(postId)
        val postStats = mapOf(
            "likecount" to 0,
        )
        databaseRef.setValue(postStats)
        .addOnSuccessListener {
            callback()
        }
        .addOnFailureListener { e ->
            e.printStackTrace()
            callback()
        }
    }

    private fun notifyStatus(action: NotificationService.ACTION, content: String) {
        val intent = Intent(this, NotificationService::class.java).apply {
            this.action = action.name
            putExtra("content", content)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
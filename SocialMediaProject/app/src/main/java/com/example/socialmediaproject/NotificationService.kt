package com.example.socialmediaproject

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class NotificationService: Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action)
        {
            ACTION.START.toString()->{
                val content=intent.getStringExtra("content") ?: ""
                Start(content)
                startUploadWorker()
            }
            ACTION.STOP.toString()->stopSelf()
            ACTION.UPDATE.toString()->{
                val content=intent.getStringExtra("content") ?: ""
                Update(content)
            }
        }
        return START_STICKY
    }

    enum class ACTION {
        START,
        STOP,
        UPDATE
    }

    private fun Start(content: String)
    {
        CreateNotificationChannel()
        val notification=NotificationCompat.Builder(this, "Notifications")
            .setSmallIcon(R.drawable.uploadicon)
            .setContentTitle("Thông báo")
            .setContentText(content)
            .build()
        startForeground(1, notification)
    }

    private fun Update(content: String)
    {
        val notification=NotificationCompat.Builder(this, "Notifications")
            .setSmallIcon(R.drawable.uploadicon)
            .setContentTitle("Thông báo")
            .setContentText(content)
            .build()
        startForeground(1, notification)
    }

    private fun CreateNotificationChannel()
    {
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {
            val channel= NotificationChannel("Notifications", "Thông báo", NotificationManager.IMPORTANCE_HIGH)
            val notificationmanager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationmanager.createNotificationChannel(channel)
        }
    }

    private fun startUploadWorker() {
        val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(
                    Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // Chỉ chạy khi có mạng
                    .build()
            ).build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}
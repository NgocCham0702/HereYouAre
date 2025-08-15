package com.cham.appvitri.services// File: FirestoreListenerService.kt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.cham.appvitri.R // <-- Sửa lại R.drawable.logo nếu cần

class FirestoreListenerService : Service() {

    private var firestoreListener: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("ListenerService", "Service được tạo (Created)")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ListenerService", "Service đã bắt đầu (Started)")
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        startListeningForMessages()
        return START_STICKY
    }

    private fun startListeningForMessages() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.w("ListenerService", "Người dùng chưa đăng nhập. Dừng service.")
            stopSelf()
            return
        }

        firestoreListener?.remove()

        // Giả định:
        // - Bạn có collection "chats".
        // - Mỗi document chat có một array "participants" chứa ID của 2 người.
        // - Mỗi document chat có trường "lastMessage" và "lastMessageSenderId".
        firestoreListener = FirebaseFirestore.getInstance()
            .collection("chats") // <-- KIỂM TRA TÊN COLLECTION NÀY
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ListenerService", "Lắng nghe thất bại.", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    // Chúng ta chỉ quan tâm khi một document chat được CẬP NHẬT (có tin nhắn mới)
                    if (dc.type == DocumentChange.Type.MODIFIED) {
                         val chatData = dc.document.data
                         val lastMessage = chatData["lastMessage"] as? String ?: ""
                         val lastMessageSenderId = chatData["lastMessageSenderId"] as? String ?: ""

                         // Chỉ hiện thông báo nếu tin nhắn cuối cùng không phải do mình gửi
                         if (lastMessage.isNotEmpty() && lastMessageSenderId != currentUserId) {
                             Log.d("ListenerService", "Phát hiện tin nhắn mới: $lastMessage")
                             showNewMessageNotification(lastMessage)
                         }
                    }
                }
            }
    }

    private fun showNewMessageNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "new_message_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Tin nhắn mới", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bạn có tin nhắn mới")
            .setContentText(message)
            .setSmallIcon(R.drawable.logo) // <-- ĐẢM BẢO ICON NÀY TỒN TẠI
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NEW_MESSAGE_NOTIFICATION_ID, notification)
    }

    private fun createForegroundNotification(): Notification {
        val channelId = "listener_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Trạng thái ứng dụng", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Appvitri đang chạy")
            .setContentText("Đang lắng nghe tin nhắn mới...")
            .setSmallIcon(R.drawable.logo)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove()
        Log.d("ListenerService", "Service đã bị hủy (Destroyed)")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val NEW_MESSAGE_NOTIFICATION_ID = 2
    }
}
package com.cham.appvitri// File: app/src/main/java/com/your/package/name/MyFirebaseMessagingService.kt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Hàm này sẽ được gọi khi có một tin nhắn mới từ FCM gửi đến.
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Luôn sử dụng 'data' payload
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: $data")

            // --- PHẦN CHỈNH SỬA BẮT ĐẦU TỪ ĐÂY ---

            // Thêm một trường 'type' để phân biệt các loại thông báo
            when (data["type"]) {
                "FRIEND_REQUEST" -> {
                    val title = data["title"] ?: "Thông báo"
                    val body = data["body"] ?: "Bạn có một thông báo mới."
                    // Khi nhấn vào thông báo mời kết bạn, ta sẽ mở màn hình chính
                    // hoặc một màn hình danh sách bạn bè cụ thể
                    showFriendRequestNotification(title, body)
                }
                "NEW_MESSAGE" -> {
                    val title = data["senderName"] ?: "Tin nhắn mới"
                    val body = data["messageBody"] ?: "Bạn có tin nhắn mới."
                    val conversationId = data["conversationId"]
                    if (conversationId != null) {
                        showChatNotification(title, body, conversationId)
                    }
                }
                else -> {
                    // Xử lý các loại thông báo chung khác nếu có
                }
            }
        }
    }

    /**
     * Hiển thị thông báo cho LỜI MỜI KẾT BẠN
     */
    private fun showFriendRequestNotification(title: String, body: String) {
        // Intent để mở MainActivity khi nhấn vào
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Bạn có thể thêm extra để điều hướng đến tab/màn hình bạn bè
            // ví dụ: putExtra("NAVIGATE_TO", "FRIENDS_SCREEN")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "friend_request_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lời mời kết bạn", // Tên kênh người dùng thấy
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(1, notificationBuilder.build()) // Dùng ID cố định cho loại thông báo này
    }

    /**
     * Hiển thị thông báo cho TIN NHẮN MỚI (Giữ lại logic cũ của bạn)
     */
    private fun showChatNotification(title: String, body: String, conversationId: String) {
        // Logic này của bạn đã tốt, chỉ cần đổi tên hàm cho rõ ràng
        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("CONVERSATION_ID", conversationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, conversationId.hashCode(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = "chat_notification_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Channel đã được bạn tạo ở MainActivity, nhưng tạo lại ở đây để chắc chắn
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Tin Nhắn Mới",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(conversationId.hashCode(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        // Chúng ta sẽ gọi hàm lưu token từ nơi khác, không cần xử lý ở đây
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
    // Hàm này được gọi khi FCM cấp một token mới cho thiết bị.
    // Bạn phải gửi token này lên server của mình để biết gửi thông báo đến đâu.
    /**
     * Hàm xây dựng và hiển thị một thông báo đơn giản.
     */
}
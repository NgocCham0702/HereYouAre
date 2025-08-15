package com.cham.appvitri// File: app/src/main/java/com/your/package/name/ChatActivity.kt

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val conversationIdTextView: TextView = findViewById(R.id.conversationIdTextView)

        // Lấy dữ liệu được gửi từ PendingIntent
        val conversationId = intent.getStringExtra("CONVERSATION_ID")

        if (conversationId != null) {
            // Hiển thị ID lên màn hình để kiểm tra
            conversationIdTextView.text = "ID cuộc trò chuyện: $conversationId"

            // Tại đây, bạn sẽ viết logic để tải và hiển thị tin nhắn
            // dựa trên 'conversationId' này từ database hoặc API
            loadMessages(conversationId)

        } else {
            // Xử lý trường hợp không nhận được ID
            Toast.makeText(this, "Lỗi: Không tìm thấy ID cuộc trò chuyện.", Toast.LENGTH_LONG).show()
            finish() // Đóng Activity nếu không có dữ liệu
        }
    }

    private fun loadMessages(convId: String) {
        // TODO: Viết code để tải tin nhắn cho cuộc trò chuyện này
        Log.d("ChatActivity", "Đang tải tin nhắn cho: $convId")
    }
}
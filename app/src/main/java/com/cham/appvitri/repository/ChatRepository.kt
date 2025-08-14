package com.cham.appvitri.repository

import android.util.Log
import com.cham.appvitri.model.ChatModel
import com.cham.appvitri.model.MessageModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val chatsCollection = firestore.collection("chats")

    /**
     * Lắng nghe danh sách các cuộc trò chuyện của người dùng hiện tại.
     */
    fun getChatListFlow(userId: String): Flow<List<ChatModel>> = callbackFlow {
        val query = chatsCollection
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val chatList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<ChatModel>()?.apply { id = doc.id }
            } ?: emptyList()
            
            trySend(chatList)
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Lấy thông tin của một cuộc trò chuyện cụ thể bằng ID của nó.
     */
    suspend fun getChatById(chatId: String): Result<ChatModel> {
        return try {
            val document = chatsCollection.document(chatId).get().await()
            val chat = document.toObject<ChatModel>()
            if (chat != null) {
                chat.id = document.id
                Result.success(chat)
            } else {
                Result.failure(Exception("Chat not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Tạo một cuộc trò chuyện 1-1 mới nếu nó chưa tồn tại.
     */
    suspend fun createChatForTwoUsers(userId1: String, userId2: String): String {
        val participants = listOf(userId1, userId2).sorted()

        val existingChat = chatsCollection
            .whereEqualTo("participants", participants)
            .whereEqualTo("isGroup", false)
            .limit(1)
            .get()
            .await()

        if (existingChat.isEmpty) {
            val newChat = ChatModel(
                participants = participants,
                isGroup = false,
                lastMessage = "Hãy bắt đầu trò chuyện!",
            )
            val newChatDocRef = chatsCollection.add(newChat).await()
            return newChatDocRef.id
        } else {
            return existingChat.documents.first().id
        }
    }
    /**
     * Lắng nghe danh sách tin nhắn trong một cuộc trò chuyện cụ thể.
     */
    fun getMessagesFlow(chatId: String): Flow<List<MessageModel>> = callbackFlow {
        val query = chatsCollection.document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<MessageModel>()?.apply { id = doc.id }
            } ?: emptyList()
            trySend(messages)
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Gửi một tin nhắn mới và cập nhật thông tin cuộc trò chuyện.
     */
    suspend fun sendMessage(chatId: String, message: MessageModel) {
        // 1. Thêm tin nhắn mới vào sub-collection 'messages'
        chatsCollection.document(chatId).collection("messages").add(message).await()

        // 2. Cập nhật 'lastMessage' của cuộc trò chuyện chính
        // Dùng FieldValue.serverTimestamp() để đảm bảo thời gian là của server
        val chatUpdate = mapOf(
            "lastMessage" to message.text,
            "lastMessageTimestamp" to FieldValue.serverTimestamp()
        )
        chatsCollection.document(chatId).update(chatUpdate).await()
    }

    suspend fun createGroupChat(
        creatorId: String,
        participantIds: List<String>,
        groupName: String
        // (Tùy chọn) bạn có thể thêm tham số groupAvatarUrl: String? ở đây nếu muốn
    ): Result<String> {
        return try {
            val allParticipants = (participantIds + creatorId).distinct()
            if (allParticipants.size < 2) {
                return Result.failure(IllegalArgumentException("Một nhóm phải có ít nhất 2 thành viên."))
            }

            // --- BẮT ĐẦU SỬA TẠI ĐÂY ---
            val newChat = ChatModel(
                participants = allParticipants,
                isGroup = true,
                groupName = groupName.trim(), // <<< THÊM TÊN NHÓM
                groupAvatarUrl = null, // <<< Tạm thời để là null, sẽ xử lý sau
                lastMessage = "Nhóm đã được tạo.",
                lastMessageTimestamp = com.google.firebase.Timestamp.now()
            )
            // --- KẾT THÚC SỬA ---

            val newChatDocRef = chatsCollection.add(newChat).await()
            Result.success(newChatDocRef.id)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Lỗi khi tạo nhóm chat: ", e)
            Result.failure(e)
        }
    }
    suspend fun hideChatForUser(chatId: String, userId: String) { // Bỏ Result<> đi cho đơn giản
        try {
            val chatRef = chatsCollection.document(chatId)
            chatRef.update("deletedBy", FieldValue.arrayUnion(userId)).await()
            // THÊM LOG NÀY
            Log.d("DELETE_DEBUG", "Đã cập nhật thành công deletedBy cho Chat ID: $chatId")
        } catch (e: Exception) {
            // THÊM LOG LỖI NÀY
            Log.e("DELETE_DEBUG", "LỖI KHI CẬP NHẬT deletedBy: ", e)
        }
    }
}

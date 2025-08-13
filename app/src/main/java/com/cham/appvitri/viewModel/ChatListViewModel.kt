package com.cham.appvitri.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.ChatRepository
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.utils.AvatarHelper
import com.cham.appvitri.view.ChatPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.mapNotNull

class ChatListViewModel : ViewModel() {

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _chatList = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chatList: StateFlow<List<ChatPreview>> = _chatList.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        val currentUserId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            chatRepository.getChatListFlow(currentUserId).collect { chats ->
                Log.d("ChatListDebug", "Tải về ${chats.size} cuộc trò chuyện. Trạng thái 'isGroup':")
                chats.forEach { chat ->
                    Log.d("ChatListDebug", "ID: ${chat.id}, isGroup: ${chat.isGroup}, Tên nhóm: ${chat.groupName}")
                }
                if (chats.isEmpty()) {
                    _chatList.value = emptyList()
                    return@collect
                }

                // Tách riêng chat nhóm và chat 1-1
                val groupChats = chats.filter { it.isGroup }
                val singleChats = chats.filter { !it.isGroup }

                // --- Xử lý chat nhóm ---
                val groupChatPreviews = groupChats.map { chat ->
                    ChatPreview(
                        id = chat.id,
                        name = chat.groupName ?: "Nhóm không tên",
                        lastMessage = chat.lastMessage ?: "",
                        timestamp = formatTimestamp(chat.lastMessageTimestamp),
                        avatarResId = com.cham.appvitri.R.drawable.img_14, // <<< THAY BẰNG ICON NHÓM CỦA BẠN
                        isGroup = true
                    )
                }

                // --- Xử lý chat 1-1 (đã được tối ưu) ---
                val otherUserIds =
                    singleChats.mapNotNull { it.participants.firstOrNull { id -> id != currentUserId } }
                if (otherUserIds.isNotEmpty()) {
                    val usersResult = userRepository.getUsersProfiles(otherUserIds)
                    if (usersResult.isSuccess) {
                        val userMap = usersResult.getOrNull()?.associateBy { it.uid } ?: emptyMap()

                        val singleChatPreviews = singleChats.mapNotNull { chat ->
                            val otherUserId = chat.participants.firstOrNull { it != currentUserId }
                            userMap[otherUserId]?.let { otherUser ->
                                ChatPreview(
                                    id = chat.id,
                                    name = otherUser.displayName ?: "Người dùng",
                                    lastMessage = chat.lastMessage ?: "",
                                    timestamp = formatTimestamp(chat.lastMessageTimestamp),
                                    avatarResId = AvatarHelper.getDrawableId(otherUser.profilePictureUrl),
                                    isGroup = false
                                )
                            }
                        }
                        // Gộp 2 danh sách lại và sắp xếp theo thời gian
                        _chatList.value = (groupChatPreviews + singleChatPreviews)
                            .sortedByDescending { it.timestamp } // Sắp xếp lại nếu cần
                    }
                } else {
                    // Nếu chỉ có chat nhóm
                    _chatList.value = groupChatPreviews.sortedByDescending { it.timestamp }
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
}
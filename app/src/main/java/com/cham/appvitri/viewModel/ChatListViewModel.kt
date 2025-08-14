package com.cham.appvitri.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.R
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.ChatRepository
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.utils.AvatarHelper
import com.cham.appvitri.view.ChatPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
            chatRepository.getChatListFlow(currentUserId)
                .catch { e ->
                    Log.e("ChatListVM", "Lỗi flow danh sách chat", e)
                }
                .collect { chatsFromFirebase ->
                    Log.d("ChatListDebug", "Flow phát ra ${chatsFromFirebase.size} chat từ Firebase.")

                    // Lọc ra các cuộc trò chuyện chưa bị người dùng này ẩn đi
                    val visibleChats = chatsFromFirebase.filter { chat ->
                        !chat.deletedBy.contains(currentUserId)
                    }

                    Log.d("ChatListDebug", "Sau khi lọc, còn ${visibleChats.size} chat hiển thị.")

                    // Tách riêng chat nhóm và chat 1-1 từ danh sách đã lọc
                    val groupChats = visibleChats.filter { it.isGroup }
                    val singleChats = visibleChats.filter { !it.isGroup }

                    // Tạo ChatPreview cho nhóm
                    val groupChatPreviews = groupChats.map { chat ->
                        ChatPreview(
                            id = chat.id,
                            name = chat.groupName ?: "Nhóm không tên",
                            lastMessage = chat.lastMessage ?: "",
                            timestamp = formatTimestamp(chat.lastMessageTimestamp),
                            avatarResId = R.drawable.img_14, // Thay bằng icon của bạn
                            isGroup = true
                        )
                    }

                    // Lấy ID của người dùng khác trong các cuộc trò chuyện 1-1
                    val otherUserIds = singleChats.mapNotNull { it.participants.firstOrNull { id -> id != currentUserId } }.distinct()

                    // Mặc định danh sách chat 1-1 là rỗng
                    var singleChatPreviews: List<ChatPreview> = emptyList()

                    // Nếu có chat 1-1, thì mới đi lấy thông tin người dùng
                    if (otherUserIds.isNotEmpty()) {
                        val usersResult = userRepository.getUsersProfiles(otherUserIds)
                        if (usersResult.isSuccess) {
                            val userMap = usersResult.getOrNull()?.associateBy { it.uid } ?: emptyMap()

                            singleChatPreviews = singleChats.mapNotNull { chat ->
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
                        }
                    }

                    // Gộp 2 danh sách lại và gán vào StateFlow
                    // Thao tác này luôn tạo ra một list mới hoàn toàn.
                    _chatList.value = (groupChatPreviews + singleChatPreviews).sortedByDescending { it.timestamp }
                }
        }
    }
    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
        fun deleteChat(chatId: String) {
            val currentUserId = authRepository.getCurrentUserId() ?: return
            viewModelScope.launch {
                chatRepository.hideChatForUser(chatId, currentUserId)
            }
        }
}
package com.cham.appvitri.viewModel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.Message
import com.cham.appvitri.model.MessageModel
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.ChatRepository
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.utils.AvatarHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// Giữ nguyên UI State của bạn, nó đã tốt rồi
data class ChatDetailUiState(
    val chatName: String = "Đang tải...",
    val chatAvatarResId: Int = AvatarHelper.getDrawableId(null),
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null, // Thêm trường error để xử lý lỗi,
    val isGroupChat: Boolean = false
)

class ChatDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private var currentUserProfile: UserModel? = null

    private val chatId: String = savedStateHandle.get<String>("chatId")!!
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val chatRepository = ChatRepository()
    private val currentUserId = authRepository.getCurrentUserId()

    // Chỉ sử dụng MỘT StateFlow cho toàn bộ UI
    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    init {
        // Chỉ cần gọi một hàm duy nhất để tải tất cả
        loadInitialData()
    }

    private fun loadInitialData() {
        if (currentUserId == null) {
            _uiState.update { it.copy(isLoading = false, error = "Lỗi: Người dùng không tồn tại.") }
            return
        }

        viewModelScope.launch {
            // Lấy thông tin chat (tên, avatar) một lần duy nhất
            loadChatInfo()
            loadCurrentUserProfile()
            // Lắng nghe tin nhắn liên tục
            listenToMessages()
        }
    }
    private fun loadCurrentUserProfile() {
        if (currentUserId == null) return
        viewModelScope.launch {
            val result = userRepository.getUserProfileOnce(currentUserId)
            if (result.isSuccess) {
                currentUserProfile = result.getOrNull()
                Log.d("CHAT_DEBUG", "Đã tải xong profile người dùng hiện tại: ${currentUserProfile?.displayName}")
            } else {
                Log.e("CHAT_DEBUG", "Lỗi khi tải profile người dùng hiện tại.")
            }
        }
    }
    // Hàm này chỉ chạy 1 lần để lấy tên và avatar
    private fun loadChatInfo() {
        viewModelScope.launch {
            val chatResult = chatRepository.getChatById(chatId)
            if (chatResult.isSuccess) {
                val chat = chatResult.getOrNull()
                if (chat == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Không tìm thấy cuộc trò chuyện.") }
                    return@launch
                }

                if (chat.isGroup) {
                    // Xử lý khi là chat nhóm
                    _uiState.update {
                        it.copy(
                            chatName = chat.groupName ?: "Nhóm không tên",
                            chatAvatarResId = com.cham.appvitri.R.drawable.img_14, // <<< THAY BẰNG ICON NHÓM CỦA BẠN
                            isGroupChat = true
                        )
                    }
                } else {
                    _uiState.update { it.copy(isGroupChat = false) }
                    // Xử lý khi là chat 1-1
                    val otherUserId = chat.participants.firstOrNull { it != currentUserId }
                    if (otherUserId != null) {
                        val userResult = userRepository.getUserProfileOnce(otherUserId)
                        if (userResult.isSuccess) {
                            val otherUser = userResult.getOrNull()!!
                            _uiState.update {
                                it.copy(
                                    chatName = otherUser.displayName ?: "Người dùng",
                                    chatAvatarResId = AvatarHelper.getDrawableId(otherUser.profilePictureUrl)
                                )
                            }
                        } else {
                            _uiState.update { it.copy(chatName = "Không tìm thấy người dùng") }
                        }
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Không thể tải thông tin cuộc trò chuyện.") }
            }
        }
    }

    // Hàm này chạy liên tục để nhận tin nhắn mới
    private fun listenToMessages() {
        viewModelScope.launch {
            chatRepository.getMessagesFlow(chatId)
                .catch { exception ->
                    Log.e("CHAT_DEBUG", "Lỗi khi lắng nghe tin nhắn: ", exception)
                    _uiState.update { it.copy(isLoading = false, error = "Không thể tải tin nhắn.") }
                }
                .collect { messageModels ->
                    val uiMessages = messageModels.map { model ->
                        Message(
                            id = model.id,
                            text = model.text,
                            timestamp = formatTimestamp(model.timestamp),
                            isFromMe = model.senderId == currentUserId,
                            // Map dữ liệu mới
                            senderName = model.senderName,
                            senderAvatarResId = AvatarHelper.getDrawableId(model.senderAvatarUrl)
                        )
                    }
                    _uiState.update { it.copy(messages = uiMessages, isLoading = false) }
                }
        }
    }

    fun sendMessage(text: String) {
        // Kiểm tra text và cả currentUserProfile đã được tải chưa
        if (text.isBlank() || currentUserId == null || currentUserProfile == null) return
        if (currentUserProfile == null) {
            Log.e("CHAT_DEBUG", "Không thể gửi tin nhắn vì currentUserProfile là null!")
            return
        }
        viewModelScope.launch {
            val message = MessageModel(
                senderId = currentUserId,
                text = text.trim(),
                // Đính kèm thông tin người gửi
                senderName = currentUserProfile?.displayName,
                senderAvatarUrl = currentUserProfile?.profilePictureUrl
            )
            chatRepository.sendMessage(chatId, message)
        }
    }

    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
}
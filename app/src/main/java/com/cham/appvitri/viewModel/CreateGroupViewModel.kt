package com.cham.appvitri.viewModel // Hoặc package ViewModel của bạn

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.ChatRepository
import com.cham.appvitri.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateGroupViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()
    private val chatRepository = ChatRepository()

    private val _friends = MutableStateFlow<List<UserModel>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _selectedFriendIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFriendIds = _selectedFriendIds.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        val currentUserId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = userRepository.getFriends(currentUserId)
            if (result.isSuccess) {
                _friends.value = result.getOrNull() ?: emptyList()
            } else {
                _error.value = "Không thể tải danh sách bạn bè."
            }
        }
    }

    fun toggleFriendSelection(friendId: String) {
        _selectedFriendIds.update { currentSelection ->
            if (currentSelection.contains(friendId)) {
                currentSelection - friendId
            } else {
                currentSelection + friendId
            }
        }
    }

    fun createGroup(groupName: String, onGroupCreated: (String) -> Unit) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId == null) {
            _error.value = "Người dùng không hợp lệ."
            return
        }
        if (groupName.isBlank()) {
            _error.value = "Vui lòng nhập tên nhóm."
            return
        }
        if (_selectedFriendIds.value.isEmpty()) {
            _error.value = "Vui lòng chọn ít nhất một người bạn."
            return
        }

        viewModelScope.launch {
            val result = chatRepository.createGroupChat(
                creatorId = currentUserId,
                participantIds = _selectedFriendIds.value.toList(),
                groupName = groupName
            )

            if (result.isSuccess) {
                val newChatId = result.getOrNull()
                if (newChatId != null) {
                    onGroupCreated(newChatId)
                }
            } else {
                _error.value = "Tạo nhóm thất bại: ${result.exceptionOrNull()?.message}"
                Log.e("CreateGroupVM", "Lỗi tạo nhóm: ", result.exceptionOrNull())
            }
        }
    }
}

// LƯU Ý: Bạn sẽ cần thêm hàm `getFriends` vào `UserRepository.kt` của bạn.
// Dưới đây là ví dụ:
/*
suspend fun getFriends(userId: String): Result<List<UserModel>> {
    return try {
        val userDoc = usersCollection.document(userId).get().await()
        val friendUids = userDoc.toObject<UserModel>()?.friendUids ?: emptyList()
        if (friendUids.isEmpty()) {
            return Result.success(emptyList())
        }
        val friendsSnapshot = usersCollection.whereIn(FieldPath.documentId(), friendUids).get().await()
        val friends = friendsSnapshot.toObjects(UserModel::class.java)
        Result.success(friends)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
*/
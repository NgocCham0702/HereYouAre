package com.cham.appvitri.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.EventRepository
import com.cham.appvitri.repository.UserRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// UI State để quản lý trạng thái của màn hình
data class CreateEventUiState(
    val isLoading: Boolean = false,
    val eventCreationSuccess: Boolean = false,
    val errorMessage: String? = null
)

class CreateEventViewModel : ViewModel() {
    private val eventRepository = EventRepository()
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository() // <<< Cần UserRepository để lấy bạn bè

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()
    private val _friends = MutableStateFlow<List<UserModel>>(emptyList())
    val friends: StateFlow<List<UserModel>> = _friends.asStateFlow()
    private val _selectedFriendUids = MutableStateFlow<Set<String>>(emptySet())
    val selectedFriendUids: StateFlow<Set<String>> = _selectedFriendUids.asStateFlow()
    // ---------------------------------------------

    init {
        loadFriends() // Tải danh sách bạn bè khi ViewModel được tạo
    }

    // --- HÀM MỚI ---
    private fun loadFriends() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = userRepository.getFriends(userId)
            if (result.isSuccess) {
                _friends.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    // --- HÀM MỚI ---
    fun toggleFriendSelection(friendUid: String) {
        _uiState.update {
            val currentSelected = _selectedFriendUids.value
            if (currentSelected.contains(friendUid)) {
                _selectedFriendUids.value = currentSelected - friendUid
            } else {
                _selectedFriendUids.value = currentSelected + friendUid
            }
            it // Không thay đổi uiState chính
        }
    }

    fun createEvent(title: String, location: String, date: String, time: String) {
        Log.d("CREATE_EVENT_DEBUG", "Bắt đầu tạo sự kiện...")
        Log.d("CREATE_EVENT_DEBUG", "Title: '$title', Location: '$location', Date: '$date', Time: '$time'")

        val creatorUid = authRepository.getCurrentUserId()
        if (creatorUid == null) {
            _uiState.update { it.copy(errorMessage = "Lỗi: Không tìm thấy người dùng.") }
            Log.e("CREATE_EVENT_DEBUG", "Lỗi: creatorUid is null.")
            return
        }

        // Kiểm tra dữ liệu đầu vào
        if (title.isBlank() || location.isBlank() || date.isBlank() || time.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng điền đầy đủ thông tin.") }
            Log.w("CREATE_EVENT_DEBUG", "Một trong các trường bị trống. Dừng lại.")
            return
        }

        // Chuyển đổi chuỗi ngày và giờ thành đối tượng Timestamp của Firebase
        val eventTimestamp = parseDateTimeToTimestamp(date, time)
        if (eventTimestamp == null) {
            _uiState.update { it.copy(errorMessage = "Định dạng ngày hoặc giờ không hợp lệ. Vui lòng dùng dd/MM/yyyy và HH:mm.") }
            Log.w("CREATE_EVENT_DEBUG", "parseDateTimeToTimestamp trả về null. Dừng lại.")
            return
        }

        Log.d("CREATE_EVENT_DEBUG", "Dữ liệu hợp lệ. Chuẩn bị gọi repository...")

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = eventRepository.createEvent(
                creatorUid = creatorUid,
                title = title,
                address = location,
                eventTimestamp = eventTimestamp,
                invitedFriendUids = _selectedFriendUids.value.toList() // <<< TRUYỀN DANH SÁCH BẠN ĐÃ CHỌN
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, eventCreationSuccess = true) }
                Log.d("CREATE_EVENT_DEBUG", "THÀNH CÔNG! Đã tạo sự kiện với ID: ${result.getOrNull()}")
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Tạo sự kiện thất bại: ${result.exceptionOrNull()?.message}") }
                Log.e("CREATE_EVENT_DEBUG", "Lỗi khi gọi repository: ", result.exceptionOrNull())
            }
        }
    }
    // Hàm để UI reset lại thông báo lỗi sau khi đã hiển thị
    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Hàm chuyển đổi String date & time sang Firebase Timestamp
    private fun parseDateTimeToTimestamp(dateStr: String, timeStr: String): Timestamp? {
        return try {
            // Giả sử định dạng là dd/MM/yyyy và HH:mm
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = format.parse("$dateStr $timeStr")
            if (date != null) {
                Timestamp(date)
            } else {
                null
            }
        } catch (e: Exception) {
            null // Trả về null nếu parsing thất bại
        }
    }
}
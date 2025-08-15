package com.cham.appvitri.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.EmergencyContact
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.SOSRepository
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.utils.AvatarHelper
import com.cham.appvitri.utils.LocationHelper
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Data class cho UI State
data class SosUiState(
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val timeLeft: Int = 10,
    val isCancelled: Boolean = false,
    val statusMessage: String? = null
)

// SỬA LẠI ĐỊNH NGHĨA CLASS
class SOSViewModel(application: Application) : AndroidViewModel(application) {

    // Khởi tạo LocationHelper bằng context của Application
    private val locationHelper = LocationHelper(application)

    private val sosRepository = SOSRepository()
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private var activeSosSessionId: String? = null
    private var countdownJob: Job? = null

    init {
        Log.d("SOS_DEBUG", "SOSViewModel đã được khởi tạo (init).")
        loadFriendsAsContacts()
        startCountdown()
    }

    private fun loadFriendsAsContacts() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            val friendsResult = userRepository.getFriends(currentUserId)
            if (friendsResult.isSuccess) {
                val friends = friendsResult.getOrNull() ?: emptyList()
                val contacts = friends.take(4).map { user ->
                    EmergencyContact(
                        name = user.displayName ?: "Bạn bè",
                        avatarResId = AvatarHelper.getDrawableId(user.profilePictureUrl)
                    )
                }
                _uiState.update { it.copy(emergencyContacts = contacts) }
            } else {
                Log.e("SOSViewModel", "Không thể tải danh sách bạn bè")
            }
        }
    }

    // ... các hàm còn lại không có gì thay đổi lớn ...
    // (Tôi đã sửa lại một chút trong sendSosSignal để tránh lỗi)

    private fun startCountdown() {
        Log.d("SOS_DEBUG", "Bắt đầu đếm ngược...")
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.timeLeft > 0) {
                delay(1000)
                _uiState.update { it.copy(timeLeft = it.timeLeft - 1) }
            }
            if (!_uiState.value.isCancelled) {
                Log.d("SOS_DEBUG", "Đếm ngược kết thúc. Chuẩn bị gửi tín hiệu.")
                sendSosSignal()
            }
        }
    }

    // ĐÃ SỬA LẠI ĐỂ DÙNG `latLng`
    private suspend fun sendSosSignal() {
        if (_uiState.value.isCancelled) {
            Log.d("SOS_DEBUG", "SOS đã bị hủy, không gửi tín hiệu.")
            return
        }

        Log.d("SOS_DEBUG", "Bắt đầu gửi tín hiệu SOS...")
        _uiState.update { it.copy(statusMessage = "Đang lấy vị trí của bạn...") }

        val latLng = locationHelper.fetchCurrentLocation()
        if (latLng == null) {
            _uiState.update { it.copy(statusMessage = "Lỗi: Không thể lấy được vị trí. Vui lòng kiểm tra GPS.") }
            Log.e("SOS_DEBUG", "fetchCurrentLocation trả về null.")
            cancelSos()
            return
        }

        val currentLocation = GeoPoint(latLng.latitude, latLng.longitude)
        Log.d("SOS_DEBUG", "Đã lấy vị trí THẬT: $currentLocation")

        val currentUserId = authRepository.getCurrentUserId() ?: return
        val friendsResult = userRepository.getFriends(currentUserId)

        if(friendsResult.isFailure) {
            Log.e("SOS_DEBUG", "Lỗi khi lấy danh sách bạn bè: ", friendsResult.exceptionOrNull())
            _uiState.update { it.copy(statusMessage = "Lỗi: Không lấy được danh sách bạn bè.") }
            return
        }

        val friendIds = friendsResult.getOrNull()?.map { it.uid } ?: emptyList()
        if (friendIds.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Lỗi: Bạn không có liên hệ khẩn cấp nào.") }
            Log.w("SOS_DEBUG", "Không có bạn bè, dừng gửi SOS.")
            return
        }

        Log.d("SOS_DEBUG", "Chuẩn bị gọi sosRepository.startSosSession...")
        val result = sosRepository.startSosSession(friendIds, currentLocation)

        if (result.isSuccess) {
            activeSosSessionId = result.getOrNull()
            _uiState.update { it.copy(statusMessage = "Đã gửi tín hiệu SOS thành công!") }
            Log.d("SOS_DEBUG", "THÀNH CÔNG! Đã tạo phiên SOS với ID: $activeSosSessionId")
        } else {
            _uiState.update { it.copy(statusMessage = "Lỗi: Không thể gửi tín hiệu SOS.") }
            Log.e("SOS_DEBUG", "Lỗi khi tạo phiên SOS: ", result.exceptionOrNull())
        }
    }

    fun cancelSos() {
        countdownJob?.cancel()
        _uiState.update { it.copy(isCancelled = true) }

        if (activeSosSessionId != null) {
            viewModelScope.launch {
                sosRepository.cancelSosSession(activeSosSessionId!!)
                _uiState.update { it.copy(statusMessage = "Bạn đã hủy tín hiệu SOS.") }
            }
        } else {
            _uiState.update { it.copy(statusMessage = "Đã hủy. Tín hiệu chưa được gửi đi.") }
        }
    }
}
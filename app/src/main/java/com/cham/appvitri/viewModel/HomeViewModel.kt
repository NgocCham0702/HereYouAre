
// file: com/cham/appvitri/viewModel/HomeViewModel.kt
package com.cham.appvitri.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.UserModel // Import UserModel
import com.cham.appvitri.repository.UserRepository // Import UserRepository
import com.cham.appvitri.utils.LocationHelper
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job // <<< THÊM IMPORT NÀY
import kotlinx.coroutines.flow.flatMapLatest
// Lớp State mới để chứa tất cả trạng thái của HomeScreen
data class HomeUiState(
    val userModel: UserModel? = null,
    val userLocation: LatLng? = null,
    val navigateTo: String? = null,
    val friends: List<UserModel> = emptyList()
)

class HomeViewModel(
    private val locationHelper: LocationHelper,
    private val userRepository: UserRepository // Thêm UserRepository
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var currentUserId: String? = null

    // --- State Mới ---
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    // ----------------

    private var hasZoomedOnAppStart = false
    // <<< THÊM BIẾN NÀY ĐỂ QUẢN LÝ VIỆC LẮNG NGHE BẠN BÈ >>>
    private var friendsListenerJob: Job? = null
    fun initialize(userId: String) {
        if (this.currentUserId == userId) return // Chỉ khởi tạo một lần

        this.currentUserId = userId
        startTrackingLocation()
        startListeningToUserProfile() // Bắt đầu lắng nghe hồ sơ người dùng
    }

    private fun startTrackingLocation() {
        currentUserId ?: return
        viewModelScope.launch {
            locationHelper.trackLocation()
                .catch { e -> e.printStackTrace() }
                .collect { latLng ->
                    // Cập nhật State
                    _uiState.update { it.copy(userLocation = latLng) }
                    updateLocationInFirebase(latLng)

                    if (!hasZoomedOnAppStart) {
                        zoomToCurrentLocation()
                        hasZoomedOnAppStart = true
                    }
                }
        }
    }

    // --- HÀM MỚI ĐỂ LẮNG NGHE AVATAR VÀ THÔNG TIN KHÁC ---
    private fun startListeningToUserProfile() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            userRepository.getUserProfileFlow(userId).collect { user ->
                // Mỗi khi có thay đổi trên Firestore, cập nhật userModel trong state
                _uiState.update { it.copy(userModel = user) }
                // Mỗi khi thông tin user thay đổi (ví dụ có bạn mới),
                // hãy bắt đầu lắng nghe danh sách bạn bè mới.
                val friendUids = user?.friendUids ?: emptyList()
                startListeningToFriendsLocation(friendUids)
            }
        }
    }
    // ----------------------------------------------------
    private fun startListeningToFriendsLocation(friendUids: List<String>) {
        // Hủy job lắng nghe cũ trước khi tạo job mới để tránh chạy song song
        friendsListenerJob?.cancel()

        friendsListenerJob = viewModelScope.launch {
            // Sử dụng hàm mới từ UserRepository
            userRepository.getFriendsProfileFlow(friendUids)
                .catch { e -> Log.e("HomeViewModel", "Error listening to friends", e) }
                .collect { updatedFriends ->
                    // Cập nhật danh sách bạn bè vào state
                    _uiState.update { it.copy(friends = updatedFriends) }
                }
        }
    }

    private fun updateLocationInFirebase(latLng: LatLng) {
        currentUserId?.let { uid ->
            val locationData = mapOf(
                "latitude" to latLng.latitude,
                "longitude" to latLng.longitude,
                "lastUpdated" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(uid)
                .set(locationData, SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.e("HomeViewModel", "Failed to update location", e)
                }
        }
    }

    fun zoomToCurrentLocation() {
        if (_uiState.value.userLocation != null) {
            _uiState.update { it.copy(navigateTo = "zoomToLocation") }
        }
    }

    fun onNavigated() {
        _uiState.update { it.copy(navigateTo = null) }
    }
}
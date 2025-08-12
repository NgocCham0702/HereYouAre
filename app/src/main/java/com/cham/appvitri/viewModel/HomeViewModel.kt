
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

// Lớp State mới để chứa tất cả trạng thái của HomeScreen
data class HomeUiState(
    val userModel: UserModel? = null,
    val userLocation: LatLng? = null,
    val navigateTo: String? = null
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
            }
        }
    }
    // ----------------------------------------------------

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
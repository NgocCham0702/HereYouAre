
// file: com/cham/appvitri/viewModel/HomeViewModel.kt
package com.cham.appvitri.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.UserModel // Import UserModel
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.SOSRepository
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
    val friends: List<UserModel> = emptyList(),
    val activeSosAlert: ActiveSosAlert? = null, // <<< THÊM
    val cameraTargetLocation: LatLng? = null // <<< DÙNG MỘT BIẾN RIÊNG CHO TỌA ĐỘ
)
data class ActiveSosAlert(
    val sessionId: String,
    val requestingUserId: String,
    val requestingUserName: String,
    val location: LatLng
)
class HomeViewModel(
    private val locationHelper: LocationHelper,
    private val userRepository: UserRepository,
    private val sosRepository: SOSRepository
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var currentUserId: String? = null
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var friendsListenerJob: Job? = null
    private var hasZoomedOnAppStart = false

    fun initialize(userId: String) {
        if (this.currentUserId == userId) return
        this.currentUserId = userId

        startTrackingLocation()
        startListeningToUserProfile()
        listenForIncomingSos()
    }

    private fun startListeningToUserProfile() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            userRepository.getUserProfileFlow(userId).collect { user ->
                _uiState.update { it.copy(userModel = user) }
                val friendUids = user?.friendUids ?: emptyList()
                startListeningToFriendsLocation(friendUids)
            }
        }
    }

    private fun startListeningToFriendsLocation(friendUids: List<String>) {
        friendsListenerJob?.cancel()
        friendsListenerJob = viewModelScope.launch {
            userRepository.getFriendsProfileFlow(friendUids)
                .catch { e -> Log.e("HomeViewModel", "Error listening to friends", e) }
                .collect { updatedFriends ->
                    _uiState.update { it.copy(friends = updatedFriends) }
                }
        }
    }

    private fun listenForIncomingSos() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            sosRepository.getIncomingSosFlow(userId).collect { sosSessions ->
                val latestUiState = _uiState.value

                val newAlert = sosSessions.firstOrNull()?.let { session ->
                    ActiveSosAlert(
                        sessionId = session.id,
                        requestingUserId = session.requestingUserId!!,
                        requestingUserName = session.requestingUserName ?: "Một người bạn",
                        location = LatLng(session.lastKnownLocation!!.latitude, session.lastKnownLocation.longitude)
                    )
                }

                val shouldZoomToSos = newAlert != null && newAlert.sessionId != latestUiState.activeSosAlert?.sessionId

                _uiState.update {
                    it.copy(
                        activeSosAlert = newAlert,
                        // Nếu cần zoom, cập nhật cả hai trường
                        navigateTo = if (shouldZoomToSos) "zoomToTarget" else it.navigateTo,
                        cameraTargetLocation = if (shouldZoomToSos) newAlert?.location else it.cameraTargetLocation
                    )
                }
            }
        }
    }

    // HÀM CŨ CỦA BẠN - ĐÃ SỬA LẠI ĐỂ HOẠT ĐỘNG
    fun zoomToCurrentLocation() {
        _uiState.value.userLocation?.let { myLocation ->
            _uiState.update {
                it.copy(
                    navigateTo = "zoomToTarget",
                    cameraTargetLocation = myLocation
                )
            }
        }
    }

    // HÀM CŨ CỦA BẠN - GIỮ NGUYÊN
    fun onNavigated() {
        _uiState.update { it.copy(navigateTo = null, cameraTargetLocation = null) }
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

    // +++ THÊM HÀM MỚI ĐỂ LẮNG NGHE SOS +++
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

    fun onCameraMoved() {
        _uiState.update { it.copy(navigateTo = null) }
    }
}
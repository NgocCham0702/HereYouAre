//// file: com/cham/appvitri/viewModel/HomeViewModel.kt
//package com.cham.appvitri.viewModel
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.cham.appvitri.utils.LocationHelper // Import helper của bạn
//import com.google.android.gms.maps.model.LatLng
//import com.google.firebase.Timestamp
//import com.google.firebase.firestore.FieldValue
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.GeoPoint
//import com.google.firebase.firestore.SetOptions
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.launch
//import java.util.Date
//
//// ViewModel bây giờ nhận LocationHelper qua constructor
//class HomeViewModel(private val locationHelper: LocationHelper) : ViewModel() {
//
//    private val firestore = FirebaseFirestore.getInstance()
//    private var currentUserId: String? = null
//
//    // Vị trí người dùng
//    private val _userLocation = MutableStateFlow<LatLng?>(null)
//    val userLocation = _userLocation.asStateFlow()
//
//    // Trạng thái để ra lệnh cho UI zoom camera
//    private val _navigateTo = MutableStateFlow<String?>(null)
//    val navigateTo = _navigateTo.asStateFlow()
//    private var hasZoomedOnce = false
//
//    // Hàm này sẽ được gọi từ UI khi đã có quyền
//    fun startTrackingLocation(userId: String) {
//        if (this.currentUserId != null) return
//
//        this.currentUserId = userId
//
//        // Khởi chạy một coroutine để lắng nghe vị trí từ Flow
//        viewModelScope.launch {
//            locationHelper.trackLocation() // Gọi hàm trackLocation() từ helper của bạn
//                .catch { e ->
//                    // Xử lý lỗi nếu có
//                    e.printStackTrace()
//                }
//                .collect { latLng ->
//                    // Mỗi khi có vị trí mới, cập nhật StateFlow
//                    _userLocation.value = latLng
//                    // Và gửi lên Firebase
//                    updateLocationInFirebase(latLng)
//                    // Nếu chưa zoom, thì zoom ngay
//                    if (!hasZoomedOnce) {
//                        zoomToCurrentLocation()
//                        hasZoomedOnce = true
//                    }
//                }
//        }
//    }
//
//    private fun updateLocationInFirebase(latLng: LatLng) {
//        currentUserId?.let { uid ->
//            if (uid.isNotBlank()) {
//                val userLocationData = hashMapOf(
//                    "location" to GeoPoint(latLng.latitude, latLng.longitude),
//                    "lastUpdated" to FieldValue.serverTimestamp() // Dùng FieldValue để kích hoạt @ServerTimestamp//Timestamp(Date()) // Dùng Timestamp của Firebase
//
//                )
//                firestore.collection("users").document(uid)
//                    .set(userLocationData, SetOptions.merge()) // Dùng set() để tạo hoặc ghi đè
//                    .addOnFailureListener {
//                        // Xử lý lỗi khi ghi lên Firebase
//                        Log.e("HomeViewModel", "that bai update location")
//
//                    }
//            }
//        }
//    }
//
//    // Hàm này được gọi khi bấm nút "location"
//    fun zoomToCurrentLocation() {
//        if (_userLocation.value != null) {
//            _navigateTo.value = "zoomToLocation"
//        }
//    }
//
//    // Hàm này được gọi sau khi UI đã thực hiện xong hành động
//    fun onNavigated() {
//        _navigateTo.value = null
//    }
//
//    // không cần onCleared vì awaitClose trong callbackFlow đã xử lý việc dọn dẹp
//}
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
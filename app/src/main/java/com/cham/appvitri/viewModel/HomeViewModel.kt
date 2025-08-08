// file: com/cham/appvitri/viewModel/HomeViewModel.kt
package com.cham.appvitri.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.utils.LocationHelper // Import helper của bạn
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// ViewModel bây giờ nhận LocationHelper qua constructor
class HomeViewModel(private val locationHelper: LocationHelper) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var currentUserId: String? = null

    // Vị trí người dùng
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation = _userLocation.asStateFlow()

    // Trạng thái để ra lệnh cho UI zoom camera
    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo = _navigateTo.asStateFlow()
    private var hasZoomedOnce = false

    // Hàm này sẽ được gọi từ UI khi đã có quyền
    fun startTrackingLocation(userId: String) {
        if (this.currentUserId != null) return

        this.currentUserId = userId

        // Khởi chạy một coroutine để lắng nghe vị trí từ Flow
        viewModelScope.launch {
            locationHelper.trackLocation() // Gọi hàm trackLocation() từ helper của bạn
                .catch { e ->
                    // Xử lý lỗi nếu có
                    e.printStackTrace()
                }
                .collect { latLng ->
                    // Mỗi khi có vị trí mới, cập nhật StateFlow
                    _userLocation.value = latLng
                    // Và gửi lên Firebase
                    updateLocationInFirebase(latLng)
                    // Nếu chưa zoom, thì zoom ngay
                    if (!hasZoomedOnce) {
                        zoomToCurrentLocation()
                        hasZoomedOnce = true
                    }
                }
        }
    }

    private fun updateLocationInFirebase(latLng: LatLng) {
        currentUserId?.let { uid ->
            if (uid.isNotBlank()) {
                val userLocationData = hashMapOf(
                    "location" to GeoPoint(latLng.latitude, latLng.longitude),
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid)
                    .set(userLocationData) // Dùng set() để tạo hoặc ghi đè
                    .addOnFailureListener {
                        // Xử lý lỗi khi ghi lên Firebase
                    }
            }
        }
    }

    // Hàm này được gọi khi bấm nút "location"
    fun zoomToCurrentLocation() {
        if (_userLocation.value != null) {
            _navigateTo.value = "zoomToLocation"
        }
    }

    // Hàm này được gọi sau khi UI đã thực hiện xong hành động
    fun onNavigated() {
        _navigateTo.value = null
    }

    // không cần onCleared vì awaitClose trong callbackFlow đã xử lý việc dọn dẹp
}
package com.cham.appvitri.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _phoneNumber = MutableStateFlow("") // Sẽ được coi là Email
    val phoneNumber = _phoneNumber.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess = _loginSuccess.asStateFlow()

    fun onPhoneNumberChange(value: String) {
        _phoneNumber.value = value
        _errorMessage.value = null // Xóa lỗi khi người dùng nhập lại
    }

    fun onPasswordChange(value: String) {
        _password.value = value
        _errorMessage.value = null // Xóa lỗi khi người dùng nhập lại
    }

    fun onLoginClicked() {
        //val email = _phoneNumber.value.trim()
        val email = "${_phoneNumber.value.trim()}@yourapp.com" // Giả sử dùng SĐT làm email

        val pass = _password.value.trim()

        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Email và mật khẩu không được để trống."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {

                val user = auth.signInWithEmailAndPassword(email, pass).await().user
                // Đăng nhập thành công, xử lý bước tiếp theo
                handleLoginSuccess(user)
            } catch (e: Exception) {
                _errorMessage.value = "Đăng nhập thất bại: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun onGoogleLoginClicked() {
        // Logic Google Sign-In sẽ được xử lý ở Activity/Composable
        // ViewModel chỉ cần biết khi nào quá trình đó thành công.
        _isLoading.value = true // Hiển thị loading khi cửa sổ Google hiện lên
        _errorMessage.value = null
    }

    // Hàm này được gọi từ UI sau khi nhận được credential từ Google
    fun signInWithGoogleCredential(credential: AuthCredential) {
        viewModelScope.launch {
            try {
                // đăng nhập vào firebase bằng credebtial của gg
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                // cập nhật hồ sơ người dùng trên firebase
                if (user != null){
                    val userProfile = hashMapOf(
                        "uid" to user.uid,
                        "displayName" to user.displayName,
                        "phoneNumber" to user.phoneNumber,
                        "email" to user.email,
                        "profilePictureUrl" to  user.photoUrl.toString()
                    )
                    db.collection("users").document(user.uid).set(userProfile).await()
                }
                // Đăng nhập/Đăng ký bằng Google thành công
                handleLoginSuccess(user)
            } catch (e: Exception) {
                _errorMessage.value = "Đăng nhập bằng Google thất bại: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Hàm này được gọi khi đăng nhập thất bại từ UI (ví dụ: người dùng đóng cửa sổ Google)
    fun onGoogleSignInFailed() {
        _isLoading.value = false
        _errorMessage.value = "Quá trình đăng nhập Google đã bị hủy."
    }

    private  fun handleLoginSuccess(user: FirebaseUser?) {
        if (user != null) {
            // Cập nhật vị trí sau khi đăng nhập thành công
            updateUserLocation(user.uid) { success ->
                if (success) {
                    Log.d("Firestore", "Cập nhật vị trí thành công ")
                    Log.d("LoginViewModel", "Cạpa nhật vị trí tc.")
                }
                // Thông báo cho UI rằng đã đăng nhập thành công
                _loginSuccess.value = true
                _isLoading.value = false
            }
        } else {
            _errorMessage.value = "Không thể lấy thông tin người dùng."
            _isLoading.value = false
        }
    }

    // Hàm lấy và cập nhật vị trí lên Firestore
    @SuppressLint("MissingPermission") // Cảnh báo: Cần đảm bảo quyền đã được cấp ở UI
     fun updateUserLocation(userId: String,onCompletion: (Boolean) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val locationData = hashMapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "lastUpdated" to FieldValue.serverTimestamp() // Dùng timestamp của server
                )

                // Lưu vào collection "users", document là userId của người dùng
                // SetOptions.merge() sẽ tạo mới nếu chưa có, hoặc cập nhật nếu đã có
                db.collection("users").document(userId)
                    .set(locationData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("Firestore", "cập nhật vị trí ")
                        onCompletion(true)
                    }
                    .addOnFailureListener { e ->
                         Log.w("Firestore", "lỗi vị trí ", e)
                        _errorMessage.value = "Lỗi khi cập nhật vị trí."
                    }
            } else {
                 Log.w("Location", "không láyd vị trí ")
                _errorMessage.value = "Không thể lấy vị trí. Vui lòng bật GPS."
                onCompletion(false)
            }
        }.addOnFailureListener {
             Log.e("Location", "lỗi vị trí .", it)
            _errorMessage.value = "Lỗi khi lấy vị trí."
            onCompletion(false)
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
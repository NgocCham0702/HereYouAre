package com.cham.appvitri.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.repository.UserRepository
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
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
        val userInput = _phoneNumber.value.trim() // Dùng tên biến chung chung hơn
        val pass = _password.value.trim()
        if (userInput.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Email/Số điện thoại và mật khẩu không được để trống."
            return
        }
        // 1. Phân loại đầu vào để xác định email đăng nhập chính xác
        val emailToSignIn: String
        val isEmailInput = userInput.contains("@") && android.util.Patterns.EMAIL_ADDRESS.matcher(userInput).matches()
        if (isEmailInput) {
            // LUỒNG 1: Người dùng đăng nhập bằng EMAIL THẬT
            emailToSignIn = userInput
        } else {
            // LUỒNG 2: Người dùng đăng nhập bằng SỐ ĐIỆN THOẠI
            // -> Chuyển đổi SĐT thành "email kỹ thuật" giống hệt lúc đăng ký
            emailToSignIn = "$userInput@auth.yourapp.com"
        }
        // --- KẾT THÚC PHẦN CHỈNH SỬA ---
        // 2. Tiến hành đăng nhập với email đã được xác định
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Sử dụng biến emailToSignIn đã được xử lý
                val user = auth.signInWithEmailAndPassword(emailToSignIn, pass).await().user
                // Đăng nhập thành công, các bước xử lý sau đó giữ nguyên
                handleLoginSuccess(user)
            } catch (e: Exception) {
                // Cải thiện thông báo lỗi
                val message = when (e) {
                    is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại."
                    is FirebaseAuthInvalidCredentialsException -> "Sai mật khẩu. Vui lòng thử lại."
                    else -> "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin."
                }
                _errorMessage.value = message
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
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Bước 1: Đăng nhập vào Firebase (giữ nguyên)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: throw IllegalStateException("Firebase user is null.")

                // Bước 2: Sử dụng UserRepository để tương tác với Firestore
                // (Nên tách logic Firestore ra Repository để tái sử dụng)
                val userRepository = UserRepository()
                val existingUserResult = userRepository.getUserProfileOnce(user.uid)

                if (existingUserResult.isFailure) {
                    // TRƯỜNG HỢP 1: ĐĂNG KÝ MỚI (User chưa có trong Firestore)

                    val newUserProfile = UserModel(
                        uid = user.uid,
                        displayName = user.displayName,
                        email = user.email,
                        phoneNumber = user.phoneNumber,
                        profilePictureUrl = user.photoUrl?.toString(),
                        personalCode = generatePersonalCode() // Giữ nguyên logic tạo mã
                    )

                    userRepository.saveUserProfile(newUserProfile).getOrThrow() // Lưu người dùng mới

                } else {
                    // TRƯỜG HỢP 2: ĐĂNG NHẬP (User đã có trong Firestore)
                    // -> Chỉ cập nhật thông tin cần thiết.
                    // Logic này thường nằm trong UserRepository, ví dụ: userRepository.updateUserInfo(...)
                    // Tạm thời làm trực tiếp ở đây:
                    val userDocRef = db.collection("users").document(user.uid)
                    val updates = mapOf(
                        "displayName" to user.displayName,
                        "profilePictureUrl" to user.photoUrl.toString()
                    )
                    userDocRef.update(updates).await()
                }

                // Bước 3: Xử lý đăng nhập thành công (giữ nguyên)
                handleLoginSuccess(user)

            } catch (e: Exception) {
                _errorMessage.value = "Đăng nhập bằng Google thất bại: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    private fun generatePersonalCode(): String {
        // 1. Định nghĩa bộ ký tự sẽ sử dụng
        // Bỏ đi các ký tự dễ nhầm lẫn như O, 0, I, l, 1
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

        // 2. Lấy ngẫu nhiên 6 ký tự từ bộ ký tự trên và ghép lại thành chuỗi
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }
    // Hàm này được gọi khi đăng nhập thất bại từ UI (ví dụ: người dùng đóng cửa sổ Google)
    fun onGoogleSignInFailed() {
        _isLoading.value = false
        _errorMessage.value = "Quá trình đăng nhập Google đã bị hủy."
    }

    private  fun handleLoginSuccess(user: FirebaseUser?) {
        if (user != null) {
            updateFcmTokenForCurrentUser()

            // Cập nhật vị trí sau khi đăng nhập thành công
            updateUserLocation(user.uid) { success ->
                if (success) {
                    Log.d("Firestore", "Cập nhật vị trí thành công ")
                    Log.d("LoginViewModel", "Cập nhật vị trí tc.")
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

    private fun updateFcmTokenForCurrentUser() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("FCM_TOKEN", "User ID is null, cannot update token.")
            return
        }

        // Khởi tạo UserRepository
        val userRepository = UserRepository()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM_TOKEN", "Fetched token: $token for user: $userId")

            // Dùng viewModelScope để chạy coroutine
            viewModelScope.launch {
                try {
                    userRepository.updateFcmToken(userId, token)
                    Log.d("FCM_TOKEN", "Token updated successfully in Firestore.")
                } catch (e: Exception) {
                    Log.e("FCM_TOKEN", "Error updating token in Firestore", e)
                    // Bạn có thể hiển thị lỗi cho người dùng nếu cần
                    // _errorMessage.value = "Không thể đăng ký nhận thông báo."
                }
            }
        }
    }
}
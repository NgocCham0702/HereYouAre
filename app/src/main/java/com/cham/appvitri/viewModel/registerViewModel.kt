package com.cham.appvitri.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.utils.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class RegistrationResult {
    object Idle : RegistrationResult()
    data class Success(val userId: String) : RegistrationResult()
    data class Error(val message: String) : RegistrationResult()
}

class RegisterViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository() // <<< THAY ĐỔI 2: Thêm UserRepository
    // States for input fields
    private val _displayName = MutableStateFlow("")
    val displayName = _displayName.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber = _phoneNumber.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword = _confirmPassword.asStateFlow()

    // States for UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // <<< THAY ĐỔI 3: Sử dụng RegistrationResult thay cho errorMessage và registrationSuccess
    private val _registrationResult = MutableStateFlow<RegistrationResult>(RegistrationResult.Idle)
    val registrationResult = _registrationResult.asStateFlow()


    // Event handlers from the View
    fun onDisplayNameChange(name: String) { _displayName.value = name }
    fun onPhoneNumberChange(phone: String) { _phoneNumber.value = phone }
    fun onPasswordChange(pass: String) { _password.value = pass }
    fun onConfirmPasswordChange(pass: String) { _confirmPassword.value = pass }

    fun onRegisterClicked(context: Context) {
        if (!validateInputs()) return

        viewModelScope.launch {
            _isLoading.value = true
            _registrationResult.value = RegistrationResult.Idle

            // --- PHẦN THAY ĐỔI BẮT ĐẦU TỪ ĐÂY ---

            // 1. Kiểm tra xem người dùng nhập email hay số điện thoại
            val userInput = _phoneNumber.value.trim()
            val emailToRegister: String
            val phoneToSave: String
            val emailToSave: String

            if (userInput.contains("@") && android.util.Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
                // Người dùng đã nhập một email hợp lệ
                emailToRegister = userInput
                emailToSave = userInput
                phoneToSave = "" // Không có số điện thoại nếu họ nhập email
            } else {
                // Người dùng đã nhập số điện thoại (hoặc chuỗi không phải email)
                emailToRegister = "$userInput@yourapp.com"
                emailToSave = emailToRegister
                phoneToSave = userInput
            }

            // 2. Sử dụng `emailToRegister` để đăng ký với Firebase Auth
            val authResult = authRepository.registerUserWithEmail(
                emailToRegister,
                _password.value
            )

            if (authResult.isFailure) {
                _registrationResult.value = RegistrationResult.Error(authResult.exceptionOrNull()?.message ?: "Lỗi không xác định")
                _isLoading.value = false
                return@launch
            }

            val firebaseUser = authResult.getOrNull() ?: run {
                _registrationResult.value = RegistrationResult.Error("Không thể lấy thông tin người dùng.")
                _isLoading.value = false
                return@launch
            }
            val userId = firebaseUser.uid

            // ... Lấy vị trí, tạo personalCode giữ nguyên ...
            val locationHelper = LocationHelper(context)
            val currentLocation = locationHelper.fetchCurrentLocation()
            val displayNameForCode = _displayName.value.trim().lowercase().filter { it.isLetterOrDigit() }
            val randomSuffix = (1000..9999).random()
            val generatedPersonalCode = "$displayNameForCode-$randomSuffix"

            // 3. Sử dụng các biến đã xử lý để tạo UserModel
            val userProfile = UserModel(
                uid = userId,
                displayName = _displayName.value,
                phoneNumber = phoneToSave, // Lưu SĐT nếu có
                email = emailToSave,       // Lưu email thật
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude,
                personalCode = generatedPersonalCode
            )

            // --- KẾT THÚC PHẦN THAY ĐỔI ---

            // Bước 4: Lưu thông tin vào Firestore (giữ nguyên)
            val saveResult = userRepository.saveUserProfile(userProfile)

            if (saveResult.isSuccess) {
                _registrationResult.value = RegistrationResult.Success(userId)
            } else {
                _registrationResult.value = RegistrationResult.Error("Đăng ký thành công nhưng không thể lưu hồ sơ.")
            }
            _isLoading.value = false
        }
    }
    // Hàm này sẽ được gọi từ Screen sau khi có kết quả từ Google
    fun onGoogleSignInResult(idToken: String, displayName: String?, email: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _registrationResult.value = RegistrationResult.Idle

            // Bước 1: Dùng idToken để đăng nhập vào Firebase
            val authResult = authRepository.signInWithGoogle(idToken) // Hàm này chúng ta sẽ thêm vào repo

            if (authResult.isFailure) {
                _registrationResult.value = RegistrationResult.Error(authResult.exceptionOrNull()?.message ?: "Lỗi xác thực với Firebase")
                _isLoading.value = false
                return@launch
            }

            val firebaseUser = authResult.getOrNull()!!
            val userId = firebaseUser.uid

            // Bước 2: Kiểm tra xem người dùng đã tồn tại trong Firestore chưa
            // (Đây là bước quan trọng để phân biệt đăng nhập và đăng ký)
            // Chúng ta cần thêm hàm getUserProfile một lần vào UserRepository
            val existingUser = userRepository.getUserProfileOnce(userId).getOrNull() // Hàm mới cần tạo

            if (existingUser != null) {
                // Người dùng đã tồn tại -> Đây là ĐĂNG NHẬP
                _registrationResult.value = RegistrationResult.Success(userId)
            } else {
                // Người dùng mới -> Đây là ĐĂNG KÝ
                // Tạo một hồ sơ mới cho họ
                val userProfile = UserModel(
                    uid = userId,
                    displayName = displayName ?: "Người dùng Google",
                    phoneNumber = firebaseUser.phoneNumber ?: "", // Thường là null với Google
                    email = email ?: "",
                    personalCode = generatePersonalCode(displayName ?: "googleuser") // Hàm tạo mã mới
                )

                val saveResult = userRepository.saveUserProfile(userProfile)
                if (saveResult.isSuccess) {
                    _registrationResult.value = RegistrationResult.Success(userId)
                } else {
                    _registrationResult.value = RegistrationResult.Error("Xác thực thành công nhưng không thể lưu hồ sơ.")
                }
            }
            _isLoading.value = false
        }
    }

    private fun generatePersonalCode(fullName: String): String {
        // 1. Lấy tên (từ cuối cùng) trong chuỗi họ tên
        val lastName = fullName.trim().split(" ").lastOrNull() ?: fullName

        // 2. Chuyển thành chữ thường và chỉ giữ lại các ký tự a-z
        val baseName = lastName.lowercase().filter { it in 'a'..'z' }

        // 3. Xử lý trường hợp tên rỗng hoặc toàn ký tự không phải a-z
        val finalBaseName = if (baseName.isNotBlank()) {
            baseName
        } else {
            "user" // Tên dự phòng
        }

        // 4. THAY ĐỔI Ở ĐÂY: Tạo 3 số ngẫu nhiên (từ 0 đến 999)
        //    .padStart(3, '0') để đảm bảo số luôn có 3 chữ số (ví dụ: 42 -> "042")
        val randomSuffix = Random.nextInt(1000).toString().padStart(3, '0')

        // 5. Ghép chúng lại
        return "$finalBaseName$randomSuffix"
    }

    private fun validateInputs(): Boolean {
        if (_displayName.value.isBlank()) {
            _registrationResult.value = RegistrationResult.Error("Vui lòng nhập họ và tên.")
            return false
        }
        if (_password.value.length < 6) {
            _registrationResult.value = RegistrationResult.Error("Mật khẩu phải có ít nhất 6 ký tự.")
            return false
        }
        if (_password.value != _confirmPassword.value) {
            _registrationResult.value = RegistrationResult.Error("Mật khẩu xác nhận không khớp.")
            return false
        }
        // Thêm các kiểm tra khác nếu cần (ví dụ: định dạng SĐT)
        return true
    }
    fun onResultConsumed() {
        _registrationResult.value = RegistrationResult.Idle
    }
}
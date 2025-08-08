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

    fun onRegisterClicked(context:Context) {
        if (!validateInputs()) return

        viewModelScope.launch {
            _isLoading.value = true
            _registrationResult.value = RegistrationResult.Idle // Reset state trước khi đăng ký mới
            // TODO: Gọi hàm đăng ký từ AuthRepository
            // Nếu đăng ký thành công, lưu thông tin người dùng vào Firestore
            // Bước 1: Đăng ký tài khoản trong Firebase Auth
            val authResult = authRepository.registerUserWithEmail(
                // Giả sử dùng SĐT làm email, hoặc bạn có thể thêm trường email
                "${_phoneNumber.value}@yourapp.com",
                _password.value
            )

            if (authResult.isFailure) {
                _registrationResult.value = RegistrationResult.Error(authResult.exceptionOrNull()?.message ?: "Lỗi không xác định")
                _isLoading.value = false
                return@launch
            }
            // Đăng ký Auth thành công, lấy uid
            val firebaseUser = authResult.getOrNull()
            if (firebaseUser == null) {
                _registrationResult.value = RegistrationResult.Error("Không thể lấy thông tin người dùng.")
                _isLoading.value = false
                return@launch
            }
            val userId = firebaseUser.uid

            // Bước 2: Lấy vị trí hiện tại
            val locationHelper = LocationHelper(context)
            val currentLocation = locationHelper.fetchCurrentLocation() // Có thể trả về null

            // Bước 3: Tạo đối tượng UserModel hoàn chỉnh
            val userProfile = UserModel(
                uid = userId,
                displayName = _displayName.value,
                phoneNumber = _phoneNumber.value,
                latitude = currentLocation?.latitude, // Gán vĩ độ, nếu null thì thôi
                longitude = currentLocation?.longitude // Gán kinh độ, nếu null thì thôi
            )

            // Bước 4: Lưu thông tin vào Firestore
            val saveResult = userRepository.saveUserProfile(userProfile)

            if (saveResult.isSuccess) {
                // THÀNH CÔNG TOÀN DIỆN!
                _registrationResult.value = RegistrationResult.Success(userId)
            } else {
                // Đăng ký Auth được nhưng lưu profile lỗi
                _registrationResult.value = RegistrationResult.Error("Đăng ký thành công nhưng không thể lưu hồ sơ.")
            }
            _isLoading.value = false
        }
    }

    fun onGoogleRegisterClicked() {
        viewModelScope.launch {
            _isLoading.value = true
            // TODO: Gọi hàm đăng ký/đăng nhập bằng Google từ AuthRepository

            _isLoading.value = false
        }
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
}
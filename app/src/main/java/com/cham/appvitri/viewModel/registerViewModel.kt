package com.cham.appvitri.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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
    // Bước validation giữ nguyên, nhưng sẽ được cải thiện ở Bước 3
    if (!validateInputs()) return

    viewModelScope.launch {
        _isLoading.value = true
        _registrationResult.value = RegistrationResult.Idle

        val userInput = _phoneNumber.value.trim()
        val isEmailInput =
            userInput.contains("@") && android.util.Patterns.EMAIL_ADDRESS.matcher(userInput)
                .matches()

        // --- BẮT ĐẦU PHẦN LOGIC MỚI ---

        if (isEmailInput) {
            // =========================================================
            //  LUỒNG 1: NGƯỜI DÙNG ĐĂNG KÝ BẰNG EMAIL THẬT
            // =========================================================
            val email = userInput

            // 1A. Kiểm tra email trong Firebase Auth
            val methodsResult = authRepository.getSignInMethodsForEmail(email)

            if (methodsResult.isSuccess) {
                val methods = methodsResult.getOrThrow()
                if (methods.isNotEmpty()) {
                    val message = when {
                        methods.contains("google.com") -> "Email này đã được đăng ký qua Google. Vui lòng đăng nhập bằng Google."
                        methods.contains("password") -> "Email này đã tồn tại. Vui lòng đăng nhập."
                        else -> "Email này đã tồn tại với phương thức đăng nhập khác."
                    }
                    _registrationResult.value = RegistrationResult.Error(message)
                    _isLoading.value = false
                    return@launch
                }
            } else {
                _registrationResult.value =
                    RegistrationResult.Error("Không thể kiểm tra tài khoản. Vui lòng thử lại.")
                _isLoading.value = false
                return@launch
            }

            // 1B. Đăng ký tài khoản Auth với email thật
            val authResult = authRepository.registerUserWithEmail(email, _password.value)
            if (authResult.isFailure) {
                _registrationResult.value = RegistrationResult.Error(
                    authResult.exceptionOrNull()?.message ?: "Lỗi đăng ký."
                )
                _isLoading.value = false
                return@launch
            }

            // 1C. Chuẩn bị dữ liệu và lưu vào Firestore
            val firebaseUser = authResult.getOrThrow()
            val userProfile = createUserProfile(
                context = context,
                userId = firebaseUser.uid,
                displayName = _displayName.value,
                phoneNumber = "", // Không có SĐT khi đăng ký bằng email
                email = email      // Lưu email thật
            )
            saveUserAndFinish(userProfile)

        } else {
            // ============================================================
            //  LUỒNG 2: NGƯỜI DÙNG ĐĂNG KÝ BẰNG SỐ ĐIỆN THOẠI
            // ============================================================
            val phoneNumber = userInput

            // 2A. Kiểm tra SĐT trong Firestore trước tiên
            val phoneExistsResult = userRepository.isPhoneNumberExists(phoneNumber)
            if (phoneExistsResult.isFailure || phoneExistsResult.getOrDefault(false)) {
                _registrationResult.value =
                    RegistrationResult.Error("Số điện thoại này đã được đăng ký.")
                _isLoading.value = false
                return@launch
            }

            // 2B. Tạo "email kỹ thuật" và đăng ký tài khoản Auth
            val technicalEmail = "$phoneNumber@auth.yourapp.com"
            val authResult = authRepository.registerUserWithEmail(technicalEmail, _password.value)
            if (authResult.isFailure) {
                _registrationResult.value =
                    RegistrationResult.Error("Không thể tạo tài khoản, vui lòng thử lại.")
                _isLoading.value = false
                return@launch
            }

            // 2C. Chuẩn bị dữ liệu và lưu vào Firestore
            val firebaseUser = authResult.getOrThrow()
            val userProfile = createUserProfile(
                context = context,
                userId = firebaseUser.uid,
                displayName = _displayName.value,
                phoneNumber = phoneNumber, // Lưu SĐT thật
                email = ""                 // Email thật để trống
            )
            saveUserAndFinish(userProfile)
        }
    }
}

// --- CÁC HÀM TRỢ GIÚP ĐỂ TRÁNH LẶP CODE ---

    suspend private fun createUserProfile(
        context: Context,
        userId: String,
        displayName: String,
        phoneNumber: String,
        email: String
    ): UserModel {
        val locationHelper = LocationHelper(context)
        val currentLocation = locationHelper.fetchCurrentLocation()
        val generatedPersonalCode = generatePersonalCode()

        return UserModel(
            uid = userId,
            displayName = displayName,
            phoneNumber = phoneNumber,
            email = email,
            latitude = currentLocation?.latitude,
            longitude = currentLocation?.longitude,
            personalCode = generatedPersonalCode
        )
    }

    private suspend fun saveUserAndFinish(userProfile: UserModel) {
        val saveResult = userRepository.saveUserProfile(userProfile)
        if (saveResult.isSuccess) {
            _registrationResult.value = RegistrationResult.Success(userProfile.uid)
        } else {
            // Rất quan trọng: Nếu lưu thất bại, tài khoản Auth vẫn được tạo.
            // Cần có cơ chế dọn dẹp (xóa tài khoản Auth) hoặc yêu cầu người dùng đăng nhập lại để hoàn tất.
            _registrationResult.value = RegistrationResult.Error("Đăng ký thành công nhưng không thể lưu hồ sơ.")
        }
        _isLoading.value = false
    }
    // Hàm này sẽ được gọi từ Screen sau khi có kết quả từ Google
    fun onGoogleSignInResult(idToken: String, displayName: String?, email: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _registrationResult.value = RegistrationResult.Idle

            // 1. Đăng nhập vào Firebase
            val authResult = authRepository.signInWithGoogle(idToken)

            if (authResult.isFailure) {
                // BẮT LỖI XUNG ĐỘT TÀI KHOẢN (LOGIC MỚI)
                val exception = authResult.exceptionOrNull()
                if (exception is FirebaseAuthUserCollisionException) {
                    // Email này đã tồn tại với một phương thức khác (ví dụ: password)
                    _registrationResult.value = RegistrationResult.Error("Tài khoản với email này đã tồn tại. Vui lòng đăng nhập bằng mật khẩu.")
                } else {
                    _registrationResult.value = RegistrationResult.Error(exception?.message ?: "Lỗi xác thực với Firebase")
                }
                _isLoading.value = false
                return@launch
            }

            val firebaseUser = authResult.getOrThrow()
            val userId = firebaseUser.uid

            // 2. Kiểm tra hồ sơ trong Firestore (giữ nguyên logic)
            val existingUserResult = userRepository.getUserProfileOnce(userId)

            if (existingUserResult.isSuccess && existingUserResult.getOrNull() != null) {
                // Người dùng đã tồn tại -> ĐĂNG NHẬP thành công
                _registrationResult.value = RegistrationResult.Success(userId)
            } else {
                // Người dùng mới -> ĐĂNG KÝ, tạo hồ sơ mới
                val userProfile = UserModel(
                    uid = userId,
                    displayName = displayName ?: "Người dùng Google",
                    phoneNumber = firebaseUser.phoneNumber ?: "",
                    email = email ?: firebaseUser.email ?: "",
                    personalCode = generatePersonalCode()
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
    private fun generatePersonalCode(): String {
        // 1. Định nghĩa bộ ký tự sẽ sử dụng
        // Bỏ đi các ký tự dễ nhầm lẫn như O, 0, I, l, 1
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

        // 2. Lấy ngẫu nhiên 6 ký tự từ bộ ký tự trên và ghép lại thành chuỗi
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun validateInputs(): Boolean {
        // 1. Kiểm tra các trường thông thường (giữ nguyên)
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

        // --- BẮT ĐẦU PHẦN NÂNG CẤP ---

        // 2. Lấy và kiểm tra trường Email/SĐT
        val userInput = _phoneNumber.value.trim() // _phoneNumber là StateFlow chứa cả email hoặc sđt
        if (userInput.isBlank()) {
            _registrationResult.value = RegistrationResult.Error("Vui lòng nhập email hoặc số điện thoại.")
            return false
        }

        // 3. Phân luồng để kiểm tra định dạng
        val isEmailInput = userInput.contains("@")

        if (isEmailInput) {
            // Nếu người dùng có vẻ như nhập email, hãy kiểm tra định dạng email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
                _registrationResult.value = RegistrationResult.Error("Định dạng email không hợp lệ.")
                return false
            }
        } else {
            // Nếu không phải email, coi như là SĐT và kiểm tra định dạng SĐT
            // Regex này khá cơ bản cho SĐT Việt Nam (10 chữ số, bắt đầu bằng 0)
            // Bạn có thể tùy chỉnh regex này cho chặt chẽ hơn nếu muốn
            val phoneRegex = Regex("^0\\d{9}$")
            if (!userInput.matches(phoneRegex)) {
                _registrationResult.value = RegistrationResult.Error("Số điện thoại không hợp lệ. (Gồm 10 số, bắt đầu bằng 0)")
                return false
            }
        }

        // Nếu tất cả các kiểm tra đều qua
        return true
    }
    fun onResultConsumed() {
        _registrationResult.value = RegistrationResult.Idle
    }
}
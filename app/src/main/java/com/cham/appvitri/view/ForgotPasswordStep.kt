package com.cham.appvitri.view// File: ForgotPasswordScreen.kt

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// ----------- VIEWMODEL BẮT ĐẦU TỪ ĐÂY -----------

// Trạng thái để quản lý các bước của giao diện
enum class ForgotPasswordStep {
    ENTER_PHONE,
    ENTER_OTP,
    RESET_PASSWORD
}

class ForgotPasswordViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // --- State quản lý các bước ---
    private val _currentStep = MutableStateFlow(ForgotPasswordStep.ENTER_PHONE)
    val currentStep = _currentStep.asStateFlow()

    // --- State cho dữ liệu người dùng nhập ---
    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber = _phoneNumber.asStateFlow()

    private val _otp = MutableStateFlow("")
    val otp = _otp.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword = _newPassword.asStateFlow()
    
    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword = _confirmPassword.asStateFlow()

    // --- State cho kết quả và lỗi ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _resetSuccess = MutableStateFlow(false)
    val resetSuccess = _resetSuccess.asStateFlow()

    // --- Biến lưu trữ thông tin từ Firebase ---
    private var verificationId: String? = null
    
    // --- Các hàm cập nhật UI State ---
    fun onPhoneNumberChanged(phone: String) { _phoneNumber.value = phone }
    fun onOtpChanged(code: String) { _otp.value = code }
    fun onNewPasswordChanged(pass: String) { _newPassword.value = pass }
    fun onConfirmPasswordChanged(pass: String) { _confirmPassword.value = pass }

    // 1. Gửi OTP đến SĐT
    fun sendOtp(activity: Activity) {
        if (_phoneNumber.value.isBlank()) {
            _errorMessage.value = "Vui lòng nhập số điện thoại."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                _isLoading.value = false
            }
            override fun onVerificationFailed(e: FirebaseException) {
                _errorMessage.value = "Gửi OTP thất bại: ${e.message}"
                _isLoading.value = false
            }
            override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = verId
                _currentStep.value = ForgotPasswordStep.ENTER_OTP
                _isLoading.value = false
            }
        }

        val phoneNumberWithCountryCode = "+84" + _phoneNumber.value.removePrefix("0")
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumberWithCountryCode)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // 2. Xác thực OTP
    fun verifyOtp() {
        if (_otp.value.length < 6) {
            _errorMessage.value = "Mã OTP phải có 6 chữ số."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        val credential = PhoneAuthProvider.getCredential(verificationId!!, _otp.value)
        
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _currentStep.value = ForgotPasswordStep.RESET_PASSWORD
            } else {
                _errorMessage.value = "Mã OTP không đúng."
            }
            _isLoading.value = false
        }
    }

    // 3. Đặt lại mật khẩu mới
    fun resetPassword() {
        val newPass = _newPassword.value
        if (newPass.length < 6) {
            _errorMessage.value = "Mật khẩu phải có ít nhất 6 ký tự."
            return
        }
        if (newPass != _confirmPassword.value) {
            _errorMessage.value = "Mật khẩu xác nhận không khớp."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null

        auth.currentUser?.updatePassword(newPass)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _resetSuccess.value = true
            } else {
                _errorMessage.value = "Đặt lại mật khẩu thất bại: ${task.exception?.message}"
            }
            _isLoading.value = false
        }
    }
}


// ----------- UI COMPOSABLE BẮT ĐẦU TỪ ĐÂY -----------

@Composable
fun ForgotPasswordScreen(
    onPasswordResetSuccess: () -> Unit // Callback để điều hướng về màn Login
) {
    // Khởi tạo ViewModel
    val viewModel: ForgotPasswordViewModel = viewModel()
    
    val currentStep by viewModel.currentStep.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resetSuccess by viewModel.resetSuccess.collectAsState()
    val context = LocalActivity.current

    // Khi đặt lại mật khẩu thành công, gọi callback
    if (resetSuccess) {
        // Dùng LaunchedEffect để đảm bảo callback chỉ được gọi 1 lần
        LaunchedEffect(Unit) {
            onPasswordResetSuccess()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (currentStep) {
                ForgotPasswordStep.ENTER_PHONE -> {
                    val phoneNumber by viewModel.phoneNumber.collectAsState()
                    Text("Nhập số điện thoại của bạn", fontSize = 20.sp, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { viewModel.onPhoneNumberChanged(it) },
                        label = { Text("Số điện thoại") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.sendOtp( context as Activity) }) {
                        Text("Gửi mã OTP")
                    }
                }
                ForgotPasswordStep.ENTER_OTP -> {
                    val otp by viewModel.otp.collectAsState()
                    Text("Nhập mã OTP", fontSize = 20.sp, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { viewModel.onOtpChanged(it) },
                        label = { Text("Mã OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.verifyOtp() }) {
                        Text("Xác nhận")
                    }
                }
                ForgotPasswordStep.RESET_PASSWORD -> {
                    val newPassword by viewModel.newPassword.collectAsState()
                    val confirmPassword by viewModel.confirmPassword.collectAsState()
                    Text("Tạo mật khẩu mới", fontSize = 20.sp, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { viewModel.onNewPasswordChanged(it) },
                        label = { Text("Mật khẩu mới") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                        label = { Text("Xác nhận mật khẩu mới") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.resetPassword() }) {
                        Text("Hoàn tất")
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
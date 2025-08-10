package com.cham.appvitri.view.login

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cham.appvitri.R
import com.cham.appvitri.viewModel.RegisterViewModel
import com.cham.appvitri.viewModel.RegistrationResult
import com.cham.appvitri.ui.theme.AppBlue
import com.cham.appvitri.ui.theme.AppDark
import com.cham.appvitri.ui.theme.AppGray
import com.cham.appvitri.ui.theme.AppWhite
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
@Composable
fun RegisterScreen(
    navController: NavController,
    registerViewModel: RegisterViewModel = viewModel()
) {
    // *** THAY ĐỔI 2: Lấy tất cả state từ ViewModel ***
    val displayName by registerViewModel.displayName.collectAsState()
    val phoneNumber by registerViewModel.phoneNumber.collectAsState()
    val password by registerViewModel.password.collectAsState()
    val confirmPassword by registerViewModel.confirmPassword.collectAsState()
    val isLoading by registerViewModel.isLoading.collectAsState()
    val registrationResult by registerViewModel.registrationResult.collectAsState()

    val context = LocalContext.current
// === BƯỚC 1.1: TẠO LAUNCHER ĐỂ NHẬN KẾT QUẢ TỪ GOOGLE SIGN-IN ===
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Kết quả trả về sẽ được xử lý ở đây
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // Lấy được tài khoản Google thành công!
            // Lấy idToken và gửi nó cho ViewModel
            account?.idToken?.let { idToken ->
                registerViewModel.onGoogleSignInResult(idToken, account.displayName, account.email)
            } ?: run {
                // Xử lý trường hợp không lấy được idToken
                Toast.makeText(context, "Không thể lấy token từ Google.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            // Đăng nhập Google thất bại
            Toast.makeText(context, "Đăng nhập Google thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // === BƯỚC 1.2: CẤU HÌNH GOOGLE SIGN-IN OPTIONS ===
    // Quan trọng: Thay thế 'YOUR_WEB_CLIENT_ID' bằng ID của bạn từ file google-services.json
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id)) // Rất quan trọng!
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // --- THAY THẾ TOÀN BỘ KHỐI LAUNCHEDEFFECT NÀY ---
    LaunchedEffect(registrationResult) {
        when (val result = registrationResult) {
            is RegistrationResult.Success -> {
                Toast.makeText(context, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()

                // Điều hướng đến home VÀ xóa các màn hình không cần thiết
                navController.navigate("home/${result.userId}") {
                    // Xóa tất cả các màn hình từ "welcome" cho đến màn hình hiện tại ("register")
                    // Điều này ngăn người dùng nhấn back để quay lại màn hình login/register
                    popUpTo("welcome") { inclusive = true }
                }
                // Quan trọng: Báo cho ViewModel đã xử lý xong để không hiển thị lại Toast
                registerViewModel.onResultConsumed()
            }

            is RegistrationResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                // Quan trọng: Báo cho ViewModel đã xử lý xong để không hiển thị lại Toast
                registerViewModel.onResultConsumed()
            }

            RegistrationResult.Idle -> {
                // Không làm gì
            }
        }
    }
    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    // Thêm verticalScroll để tránh lỗi tràn màn hình trên các thiết bị nhỏ
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo Icon",
                    modifier = Modifier.size(100.dp)
                )
                Text(
                    "Tạo tài khoản mới",
                    modifier = Modifier.padding(top = 10.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppDark
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Trường nhập họ và tên
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { registerViewModel.onDisplayNameChange(it) },
                    label = { Text("Họ và tên") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Trường nhập số điện thoại
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { registerViewModel.onPhoneNumberChange(it) },
                    label = { Text("Số điện thoại") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Trường nhập mật khẩu
                OutlinedTextField(
                    value = password,
                    onValueChange = { registerViewModel.onPasswordChange(it) },
                    label = { Text("Mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Trường xác nhận mật khẩu
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { registerViewModel.onConfirmPasswordChange(it) },
                    label = { Text("Xác nhận mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Hiển thị thông báo lỗi nếu có
                AnimatedVisibility(visible = registrationResult != null) {
                    val errorMessage = (registrationResult as? RegistrationResult.Error)?.message
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Nút đăng ký
                Button(
                    onClick = { registerViewModel.onRegisterClicked(context) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue)
                ) {
                    Text("ĐĂNG KÝ", color = AppWhite, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))

                // --- PHẦN ĐĂNG KÝ BẰNG GOOGLE ---
                Text("HOẶC", color = AppGray)
                Spacer(modifier = Modifier.height(10.dp))

                // Nút đăng ký với Google
                OutlinedButton(
                    onClick = {
                        // KHỞI CHẠY QUÁ TRÌNH ĐĂNG NHẬP
                        val signInIntent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    },                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Icon",
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        "  Đăng ký với Google",
                        modifier = Modifier.padding(start = 8.dp),
                        color = AppDark
                    )
                }
                // --- KẾT THÚC PHẦN ĐĂNG KÝ BẰNG GOOGLE ---

                Spacer(modifier = Modifier.height(24.dp))

                // Link quay lại đăng nhập
                Row {
                    Text("Đã có tài khoản? ", color = AppGray)
                    ClickableText(
                        text = AnnotatedString("Đăng nhập"),
                        style = TextStyle(
                            color = AppBlue,
                            fontWeight = FontWeight.Bold
                        ),
                        onClick = { navController.navigate("login") }
                    )
                }
            }

            // Hiệu ứng loading
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppBlue)
                }
            }
        }

    }
}

// --- Các hàm xem trước (Preview) ---

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun RegisterScreenPreview() {
    // ViewModel sẽ được tạo mặc định, không cần truyền state vào đây nữa.
    RegisterScreen(navController = rememberNavController())
}
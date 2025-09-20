package com.cham.appvitri.view.account

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cham.appvitri.R
import com.cham.appvitri.viewModel.LoginViewModel
import com.cham.appvitri.ui.theme.AppBlue
import com.cham.appvitri.ui.theme.AppDark
import com.cham.appvitri.ui.theme.AppGray
import com.cham.appvitri.ui.theme.AppWhite
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(
    navController: NavController,
    onForgotPasswordClicked: () -> Unit,
    onLoginSuccess: (String) -> Unit, // Callback mới: truyền userId sau khi thành công
    loginViewModel: LoginViewModel = viewModel()    )
{
    val phoneNumber by loginViewModel.phoneNumber.collectAsState()
    val password by loginViewModel.password.collectAsState()
    val isLoading by loginViewModel.isLoading.collectAsState()
    val errorMessage by loginViewModel.errorMessage.collectAsState()
    val loginSuccess by loginViewModel.loginSuccess.collectAsState()
    val isAdminLogin by loginViewModel.isAdminLogin.collectAsState()

    val context = LocalContext.current // Dùng để hiển thị Toast
    // Cấu hình Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // Lấy từ file google-services.json
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
    // Launcher để xử lý kết quả trả về từ Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            loginViewModel.signInWithGoogleCredential(credential)
        } catch (e: ApiException) {
            // Đăng nhập Google thất bại
            loginViewModel.onGoogleSignInFailed()
            Toast.makeText(context, "Đăng nhập Google thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    LaunchedEffect(loginSuccess, isAdminLogin) {
        if (isAdminLogin) {
            // Admin login
            Toast.makeText(context, "Đăng nhập admin thành công!", Toast.LENGTH_SHORT).show()
            navController.navigate("adminPanel") {
                popUpTo("login") { inclusive = true }
            }
        } else if (loginSuccess) {
            val userId = loginViewModel.getCurrentUserId()
            if (userId != null) {
                // Hiển thị Toast trước khi điều hướng
                Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                // Chỉ gọi callback. KHÔNG gọi navController.navigate ở đây nữa.
                // AppNavigation sẽ xử lý việc điều hướng.
                onLoginSuccess(userId)

            } else {
                // Xử lý trường hợp không lấy được userId
                Toast.makeText(context, "Lỗi: Không lấy được thông tin người dùng.", Toast.LENGTH_LONG).show()
                android.util.Log.e("LoginDebug", "Đăng nhập thành công nhưng không lấy được userId!")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo), // Thêm icon này
                contentDescription = "Logo Icon",
                modifier = Modifier.size(100.dp) //.align(Alignment.CenterHorizontally)
            )
            Text(
                "Chào mừng bạn đến với ứng dụng",
                modifier = Modifier.padding(top = 20.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppDark
            )
            Spacer(modifier = Modifier.height(30.dp))
            // Trường nhập số điện thoại
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { loginViewModel.onPhoneNumberChange(it) },// gui su kien len viewmodel
                label = { Text("Số điện thoại") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Trường nhập mật khẩu
            OutlinedTextField(
                value = password,
                onValueChange = { loginViewModel.onPasswordChange(it)},// gui su kien len viewmodel },
                label = { Text("Mật khẩu") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(20.dp))
            // Hiển thị thông báo lỗi nếu có
            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            // Nút đăng nhập
            Button(
                onClick = {loginViewModel.onLoginClicked() },
                enabled = !isLoading, // vo hieu hoa nut khi dang loading
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =AppBlue
                )
            ) {
                Text("ĐĂNG NHẬP", color = AppWhite, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onForgotPasswordClicked, // Gọi hàm điều hướng
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Quên mật khẩu?")
            }
            Text(text="Đăng ký tài khoản",
                color = AppBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { navController.navigate("register") }
            )

            Spacer(modifier = Modifier.height(15.dp))
            Text("HOẶC", color = AppGray)
            Spacer(modifier = Modifier.height(15.dp))

            // Nút đăng nhập với Google
            OutlinedButton(
                onClick = { loginViewModel.onGoogleLoginClicked()
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)},
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google), // Thêm icon này
                    contentDescription = "Google Icon",
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    "  Đăng nhập với Google",
                    modifier = Modifier.padding(start = 8.dp),
                    color = AppDark
                )
            }
            // Hiệu ứng loading sinh động
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
@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun LoginScreenPreview() {
    // ViewModel sẽ được tạo tự động, nên Preview sẽ chạy với giá trị mặc định của ViewModel
    LoginScreen( navController = rememberNavController(), onForgotPasswordClicked = {}, onLoginSuccess = {})}


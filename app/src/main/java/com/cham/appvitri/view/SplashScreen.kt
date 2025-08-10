// file: com/cham/appvitri/view/SplashScreen.kt
package com.cham.appvitri.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Một màn hình chờ đơn giản chỉ hiển thị một vòng xoay loading ở giữa.
 * Màn hình này được dùng làm điểm khởi đầu của ứng dụng để kiểm tra
 * trạng thái đăng nhập trước khi điều hướng tới Login hoặc Home.
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // Dùng màu nền của theme
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
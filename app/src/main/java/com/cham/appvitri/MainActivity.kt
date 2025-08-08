package com.cham.appvitri // Giữ đúng package name của bạn


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cham.appvitri.navigation.AppNavigation
import com.cham.appvitri.ui.theme.AppvitriTheme
import android.Manifest

class MainActivity : ComponentActivity() {

    // Trình yêu cầu quyền để lấy vị trí
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Tại đây bạn có thể kiểm tra xem quyền đã được cấp hay chưa
            val isGranted = permissions.entries.all { it.value }
            if (isGranted) {
                // Quyền đã được cấp, có thể thực hiện hành động gì đó nếu cần
            } else {
                // Quyền bị từ chối, có thể hiển thị thông báo cho người dùng
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Yêu cầu quyền vị trí khi ứng dụng khởi động
        requestLocationPermissions()

        setContent {
            AppvitriTheme { // Áp dụng theme của ứng dụng
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Chỉ cần gọi AppNavigation ở đây!
                    AppNavigation()
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}
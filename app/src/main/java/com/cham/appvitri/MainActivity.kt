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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.view.WindowCompat
import android.util.Log
import androidx.core.content.ContextCompat
import com.cham.appvitri.services.FirestoreListenerService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.messaging

class MainActivity : ComponentActivity() {

    // Trình yêu cầu quyền để lấy vị trí
//    private val requestPermissionLauncher
    private val locationPermissionLauncher =
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
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("PERMISSION", "Notification permission granted.")
            } else {
                Log.d("PERMISSION", "Notification permission denied.")
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Yêu cầu quyền vị trí khi ứng dụng khởi động
        requestLocationPermissions()
        createNotificationChannel()
        getFCMToken()
// GỌI HÀM KHỞI ĐỘNG SERVICE TẠI ĐÂY
        startMessageListenerService()
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                // Nếu có lỗi, in ra Logcat để biết
                Log.w("FCM_TOKEN", "Lấy FCM token thất bại", task.exception)
                return@addOnCompleteListener
            }

            // Nếu thành công, lấy token
            val token = task.result

            // In token ra Logcat với tag "FCM_TOKEN" để dễ dàng tìm kiếm và copy
            Log.d("FCM_TOKEN", "Token của thiết bị này là: $token")
        }
        // Cho phép ứng dụng vẽ tràn viền
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        askNotificationPermission()
    }
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "chat_notification_channel"
            val channelName = "Tin Nhắn Mới"
            val channelDescription = "Thông báo cho các tin nhắn trò chuyện mới"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("CHANNEL", "Notification channel created.")
        }
    }

    private fun getFCMToken() {
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TOKEN", "This device's token is: $token")
        }
    }
    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) return // Chưa đăng nhập

        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        val tokenData = mapOf("fcmToken" to token)

        userDocRef.set(tokenData, SetOptions.merge()) // Dùng merge để không ghi đè dữ liệu khác
            .addOnSuccessListener { Log.d("FCM_TOKEN", "Token saved to Firestore.") }
            .addOnFailureListener { e -> Log.w("FCM_TOKEN", "Error saving token", e) }
    }
    private fun startMessageListenerService() {
        // Chỉ khởi động service nếu người dùng đã đăng nhập
        if (FirebaseAuth.getInstance().currentUser != null) {
            val intent = Intent(this, FirestoreListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d("MainActivity", "Yêu cầu khởi động FirestoreListenerService.")
        } else {
            Log.d("MainActivity", "Người dùng chưa đăng nhập, không khởi động service.")
        }
    }
}
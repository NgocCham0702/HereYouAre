// file: com/cham/appvitri/view/HomeScreenUI.kt
package com.cham.appvitri.view

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cham.appvitri.R
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.utils.AvatarHelper
import com.cham.appvitri.utils.LocationHelper
import com.cham.appvitri.viewModel.HomeUiState
import com.cham.appvitri.viewModel.HomeViewModel
import com.cham.appvitri.viewModel.HomeViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.LaunchedEffect
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.maps.model.LatLng
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.cham.appvitri.repository.SOSRepository
import com.cham.appvitri.viewModel.ActiveSosAlert

@Composable
fun HomeScreen(
    userId: String,
    onSosClicked: () -> Unit,
    onAvatarClicked: () -> Unit,
    onAddFriendClicked: () -> Unit,
    onEventsClicked: () -> Unit,
    onChatClicked: () -> Unit,
) {
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState() // Quản lý camera state bên trong
    // Khởi tạo ViewModel với cả hai repository
    val factory = remember(userId) {
        HomeViewModelFactory(
            locationHelper = LocationHelper(context),
            userRepository = UserRepository(),
            sosRepository = SOSRepository() // <<< Thêm vào đây
        )
    }
    val homeViewModel: HomeViewModel = viewModel(factory = factory)

// logic nôị bộ của màn hình
    val uiState by homeViewModel.uiState.collectAsState()

    // Kiểm tra xem quyền đã được cấp hay chưa
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Chỉ bắt đầu theo dõi vị trí NẾU đã có quyền

    // Khởi tạo ViewModel khi có quyền và userId
    LaunchedEffect(hasLocationPermission, userId) {
        if (hasLocationPermission && userId.isNotBlank()) {
            homeViewModel.initialize(userId)
        }
    }

// Di chuyển camera khi có lệnh từ ViewModel
    LaunchedEffect(uiState.navigateTo) {
        // Chỉ chạy khi navigateTo có giá trị
        if (uiState.navigateTo == "zoomToTarget") {
            // Lấy tọa độ từ cameraTargetLocation thay vì userLocation
            uiState.cameraTargetLocation?.let { loc ->
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, 15f))
            }
            // Reset lại sau khi đã di chuyển
            homeViewModel.onNavigated()
        }
    }
    Scaffold(
        bottomBar = {
            AppBottomBar(
                onAddFriendClicked = onAddFriendClicked,
                onEventsClicked = onEventsClicked,
                onChatClicked = onChatClicked,
                onCenterLocationClicked = { homeViewModel.zoomToCurrentLocation() }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (hasLocationPermission) {
                MapAndOverlays(
                    //userAvatarUrl = null, // Lấy từ thông tin user sau này
                    uiState = uiState,
                    cameraPositionState = cameraPositionState,
                    onSosClicked = onSosClicked,
                    onAvatarClicked = onAvatarClicked
                )
            } else {
                // Hiển thị thông báo nếu không có quyền
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Ứng dụng cần quyền truy cập vị trí để hiển thị bản đồ. Vui lòng cấp quyền trong cài đặt ứng dụng.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

// MapAndOverlays và các composable khác giữ nguyên như trong câu trả lời trước
// ...
// (Copy/paste các hàm MapAndOverlays, AppBottomBar, BottomBarItem từ câu trả lời trước vào đây)
// ...
@Composable
fun MapAndOverlays(
    uiState: HomeUiState,
    cameraPositionState: CameraPositionState,
    onSosClicked: () -> Unit,
    onAvatarClicked: () -> Unit
) {
    val mapProperties = MapProperties(isMyLocationEnabled = false)
    val mapUiSettings = MapUiSettings(myLocationButtonEnabled = false)
    val nearbyFriendUids = uiState.nearbyFriends.map { it.uid }.toSet()

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {
            // Marker cho chính bạn
            uiState.userLocation?.let { myLocation ->
                FriendMarker(
                    position = myLocation,
                    title = "Vị trí của bạn",
                    snippet = uiState.userModel?.displayName ?: "",
                    avatarIdentifier = uiState.userModel?.profilePictureUrl
                )
            }

            // Marker cho bạn bè bình thường
            uiState.friends.forEach { friend ->
                if (friend.uid != uiState.activeSosAlert?.requestingUserId) {
                    val lat = friend.latitude
                    val lng = friend.longitude
                    if (lat != null && lng != null) {
                        // --- PHẦN LOGIC MỚI BẮT ĐẦU TỪ ĐÂY ---

                        // 1. Kiểm tra xem người bạn này có nằm trong danh sách ở gần không
                        val isNearby = nearbyFriendUids.contains(friend.uid)

                        // 2. Tùy chỉnh thông tin hiển thị dựa trên việc họ có ở gần hay không
                        val snippetText = if (isNearby) {
                            "Ở rất gần bạn!" // Tin nhắn đặc biệt
                        } else {
                            "Cập nhật lần cuối: ${friend.lastUpdated?.toDate()?.toString() ?: "N/A"}" // Thông tin bình thường
                        }
                        FriendMarker(
                            position = LatLng(lat, lng),
                            title = friend.displayName ?: "Bạn bè",
                            snippet = snippetText, // <<< SỬ DỤNG SNIPPET ĐÃ TÙY CHỈNH
                            avatarIdentifier = friend.profilePictureUrl
                        )
                    }
                }
            }

            // Marker SOS đặc biệt
            uiState.activeSosAlert?.let { alert ->
                SosMarker(position = alert.location, name = alert.requestingUserName)
            }
        }

        // --- BANNER CẢNH BÁO SOS ---
        AnimatedVisibility(
            visible = uiState.activeSosAlert != null,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            uiState.activeSosAlert?.let { alert ->
                SosAlertBanner(
                    alert = alert,
                    onClick = { /* Tạm thời chưa làm gì */ }
                )
            }
        }
        // -------------------------

        Button(
            onClick = onSosClicked,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(60.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_2),
                contentDescription = "SOS Button",
                modifier = Modifier.size(55.dp)
            )
        }

        Image(
            painter = painterResource(id = AvatarHelper.getDrawableId(uiState.userModel?.profilePictureUrl)),
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(55.dp)
                .clip(CircleShape)
                .background(Color.Yellow)
                .border(2.dp, Color.White, CircleShape)
                .clickable(onClick = onAvatarClicked)
        )
    }
}
@Composable
fun SosAlertBanner(alert: ActiveSosAlert, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Red.copy(alpha = 0.9f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = "Cảnh báo", tint = Color.White)
        Spacer(modifier = Modifier.width(25.dp))
        Text(
            text = "${alert.requestingUserName} đang gặp nguy hiểm!",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun SosMarker(position: LatLng, name: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "sos_pulse_anim"
    )

    MarkerComposable(
        state = MarkerState(position = position),
        anchor = Offset(0.5f, 0.5f)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.Red,
                    radius = size.minDimension / 2 * pulseAnimation,
                    alpha = 0.5f * (1f - pulseAnimation)
                )
                drawCircle(
                    color = Color.Red,
                    radius = (size.minDimension / 2 * pulseAnimation) * 0.7f,
                    alpha = 0.8f * (1f - pulseAnimation)
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.img_2), // Thay bằng icon SOS của bạn
                contentDescription = name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .padding(8.dp),
                tint = Color.White
            )
        }
    }
}
// <<< THAY ĐỔI 3: THÊM COMPOSABLE MỚI ĐỂ TẠO CUSTOM MARKER >>>
@Composable
fun FriendMarker(
    position: LatLng,
    title: String,
    snippet: String,
    avatarIdentifier: String?,

) {
    val context = LocalContext.current
    var bitmapDescriptor by remember { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(avatarIdentifier) {
        withContext(Dispatchers.IO) {
            // --- KÍCH THƯỚC MONG MUỐN ---
            val markerWidth = 110 // in pixels
            val markerHeight = 110 // in pixels

            // --- LẤY ẢNH NỀN VÀ AVATAR ---
            val pinDrawable = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
            val avatarDrawableId = AvatarHelper.getDrawableId(avatarIdentifier)
            val avatarDrawable = ContextCompat.getDrawable(context, avatarDrawableId)

            // Chuyển thành Bitmap với kích thước mong muốn
            val pinBitmap = pinDrawable?.toBitmap(markerWidth, markerHeight)
            // Kích thước avatar nhỏ hơn để có viền
            val avatarSize = 65
            val avatarBitmap = avatarDrawable?.toBitmap(avatarSize, avatarSize)

            if (pinBitmap != null && avatarBitmap != null) {
                // Tạo bitmap cuối cùng
                val finalBitmap = createBitmap(pinBitmap.width, pinBitmap.height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(finalBitmap)

                // --- BẮT ĐẦU VẼ ---
                // 1. Vẽ icon nền (giọt nước)
                canvas.drawBitmap(pinBitmap, 0f, 0f, null)

                // 2. Lấy phiên bản bo tròn của avatar
                val roundedAvatar = getCroppedBitmap(avatarBitmap)

                // 3. Tính toán vị trí để vẽ avatar vào giữa phần đầu của icon nền
                val left = (pinBitmap.width - roundedAvatar.width) / 2f
                val top = (pinBitmap.width - roundedAvatar.width) / 2f - 6 // Dịch lên một chút

                // 4. Vẽ avatar đã bo tròn lên trên
                canvas.drawBitmap(roundedAvatar, left, top, null)

                bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(finalBitmap)
            }
        }
    }

    bitmapDescriptor?.let {
        Marker(
            state = MarkerState(position = position),
            title = title,
            snippet = snippet,
            icon = it,
            anchor = Offset(0.5f, 1.0f)
        )
    }
}
/**
 * Composable này định nghĩa giao diện của marker.
 */
fun getCroppedBitmap(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
    val output = createBitmap(
        bitmap.width, bitmap.height, android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(output)
    val color = -0xbdbdbe
    val paint = Paint()
    val rect = Rect(0, 0, bitmap.width, bitmap.height)
    val rectF = RectF(rect)

    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color
    canvas.drawOval(rectF, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)

    return output
}
/**
 * Hàm tiện ích để chuyển một ComposeView thành Bitmap.
 */
@Composable
fun AppBottomBar(
    onAddFriendClicked: () -> Unit,
    onEventsClicked: () -> Unit,
    onChatClicked: () -> Unit,
    onCenterLocationClicked: () -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .navigationBarsPadding(), // Vẫn giữ cái này để không bị khuất

        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 10.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().offset(-10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(R.drawable.img_4, hasBorder = true,onClick = onAddFriendClicked)
            BottomBarItem(R.drawable.img_5, hasBorder = true, onClick = onEventsClicked)
            BottomBarItem(R.drawable.img_3, hasBorder = true,onClick = onChatClicked)
            BottomBarItem(R.drawable.location,hasBorder = true, onClick = onCenterLocationClicked)
        }
    }
}

@Composable
fun BottomBarItem(
    iconResId: Int,
    onClick: () -> Unit,
    hasBorder: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .then(if (hasBorder) Modifier.border(2.dp, Color(0xFF0D47A1), CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified
        )
    }
}
@Preview(name = "Marker Visual Preview", showBackground = true)
@Composable
fun MarkerVisualPreview() {
    // --- BẠN SẼ TINH CHỈNH CÁC GIÁ TRỊ DP Ở ĐÂY ---
    val markerSize = 110.dp
    val avatarSize = 65.dp
    val avatarPaddingTop = 11.dp
    // ---------------------------------------------

    Box(
        modifier = Modifier.size(markerSize),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Icon nền (giọt nước)
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_map_pin),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.primary // Hoặc màu bạn muốn
        )

        // 2. Ảnh đại diện bo tròn
        Image(
            painter = painterResource(id = AvatarHelper.getDrawableId("avatar_1")), // Dùng ảnh mẫu
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(top = avatarPaddingTop) // Dùng giá trị padding để căn chỉnh
                .size(avatarSize)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}
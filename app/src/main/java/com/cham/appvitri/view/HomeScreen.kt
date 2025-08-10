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
        HomeViewModelFactory(LocationHelper(context), UserRepository())
    }
    // --- Sử dụng ViewModelFactory để cung cấp LocationHelper cho ViewModel ---
    val locationHelper = remember { LocationHelper(context) }
    val homeViewModel: HomeViewModel = viewModel(factory = factory)

    //val viewModelFactory = remember { HomeViewModelFactory(locationHelper) }
   // val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    // --- Kết thúc ---
//lắng nghe state từ viewmodel
    //val userLocation by homeViewModel.userLocation.collectAsState()
    //val navigateTo by homeViewModel.navigateTo.collectAsState()
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
        if (uiState.navigateTo == "zoomToLocation") {
            uiState.userLocation?.let { loc ->
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, 15f))
            }
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
                    uiState=uiState,
                    onSosClicked = onSosClicked,
                    onAvatarClicked = onAvatarClicked,
                    cameraPositionState = cameraPositionState
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
    //userAvatarUrl: String?,
    uiState: HomeUiState,
    onSosClicked: () -> Unit,
    onAvatarClicked: () -> Unit,
    cameraPositionState: CameraPositionState
) {
    val mapProperties = MapProperties(isMyLocationEnabled = true)
    val mapUiSettings = MapUiSettings(myLocationButtonEnabled = false)

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {}

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

        // --- CẬP NHẬT AVATAR ĐỂ HIỂN THỊ ẢNH CỦA USER ---
        Image(
            // Dùng AvatarHelper để lấy ảnh từ định danh lưu trong userModel
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
            .size(70.dp)
            .clip(CircleShape)
            .then(if (hasBorder) Modifier.border(2.dp, Color(0xFF0D47A1), CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(55.dp),
            tint = Color.Unspecified
        )
    }
}

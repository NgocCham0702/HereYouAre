package com.cham.appvitri.view

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cham.appvitri.R
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.viewModel.AddFriendViewModel
import com.cham.appvitri.viewModel.AddFriendViewModel.FriendRequestWithReceiver
import com.cham.appvitri.viewModel.FriendRequestWithSender
import com.cham.appvitri.viewModel.FriendshipStatus
import com.cham.appvitri.viewModel.UserWithStatus
import androidx.compose.ui.platform.LocalSoftwareKeyboardController


@Composable
fun AddFriendScreen(
    onClose: () -> Unit,
    viewModel: AddFriendViewModel = viewModel()
) {
    // Lấy các trạng thái từ ViewModel
    val personalCode by viewModel.personalCode.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val receivedRequests by viewModel.receivedRequests.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val sentRequests by viewModel.sentRequests.collectAsState()

    // Hiển thị Snackbar khi có tin nhắn từ ViewModel
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown() // Đặt lại tin nhắn sau khi hiển thị
        }
    }

    Scaffold(
        topBar = { TopBar(onClose = onClose) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
            SearchSection(
                searchQuery = viewModel.searchQuery.value,
                onQueryChange = viewModel::onSearchQueryChanged,
                onSearchClick = {
                    // <<< 3. ẨN BÀN PHÍM KHI NHẤN NÚT >>>
                    keyboardController?.hide()
                    viewModel.searchUsers()}
            )

            Spacer(modifier = Modifier.height(24.dp))

            PersonalCodeSection(
                code = personalCode,
                onCopyClick = { /* Logic copy đã được chuyển vào trong */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Hiển thị nội dung động: Loading, Kết quả tìm kiếm, hoặc Danh sách mặc định
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (searchResult.isNotEmpty()) {
                SearchResultSection(
                    usersWithStatus = searchResult,
                    onAddFriend = viewModel::sendFriendRequest,
                    onCancelRequest = viewModel::cancelFriendRequest,
                    sentRequests = sentRequests
                )
            } else {
                DefaultListsSection(
                    requests = receivedRequests,
                    sentRequests = sentRequests,
                    friends = friends,
                    onAccept = viewModel::acceptFriendRequest,
                    onDecline = viewModel::declineFriendRequest,
                    onCancel = viewModel::cancelFriendRequest,
                    onDelete = viewModel::deleteFriend
                )
            }
        }
    }
}

// --- CÁC COMPOSABLE CON ĐÃ ĐƯỢC CẬP NHẬT ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(onClose: () -> Unit) {
    TopAppBar(
        title = { Text("Thêm bạn bè", fontWeight = FontWeight.Bold, color = Color.White) },
        actions = {
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD92B1D)) // Màu  cho phù hợp hơn
            ) {
                Text("Đóng")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D6EFD))
    )
}

@Composable
fun SearchSection(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        label = { Text("Nhập mã mời, sdt, email của bạn bè...") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(8.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onSearchClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D6EFD))
    ) {
        Text("Tìm kiếm", fontSize = 16.sp, modifier = Modifier.padding(vertical = 6.dp))
    }
}

@Composable
fun PersonalCodeSection(code: String, onCopyClick: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Mã cá nhân của bạn:", fontSize = 18.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(code, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                clipboardManager.setText(AnnotatedString(code))
                Toast.makeText(context, "Đã sao chép mã!", Toast.LENGTH_SHORT).show()
                onCopyClick()
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF198754))
        ) {
            Text("Sao chép mã")
        }
    }
}

// --- CÁC SECTION MỚI CHO NỘI DUNG ĐỘNG ---

@Composable
fun SearchResultSection(usersWithStatus: List<UserWithStatus>,
                        onAddFriend: (UserModel) -> Unit,
                        onCancelRequest: (AddFriendViewModel.FriendRequestWithReceiver) -> Unit,
                        sentRequests: List<AddFriendViewModel.FriendRequestWithReceiver>) {
    Column {
        Text("Kết quả tìm kiếm", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(usersWithStatus) { item ->
                UserItem(
                    name = item.user.displayName ?: "...",
                    actionButton = {
                        when (item.status) {
                            FriendshipStatus.NOT_FRIENDS -> Button(onClick = { onAddFriend(item.user) }) { Text("Kết bạn") }
                            FriendshipStatus.REQUEST_SENT -> Button(onClick = {
                                val requestToCancel = sentRequests.find { it.receiver.uid == item.user.uid }
                                requestToCancel?.let(onCancelRequest)
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Hủy") }
                            FriendshipStatus.IS_FRIEND -> Text("Bạn bè")
                            FriendshipStatus.SELF -> Text("Là bạn")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DefaultListsSection(
    requests: List<FriendRequestWithSender>,
    sentRequests: List<FriendRequestWithReceiver>,
    friends: List<UserModel>,
    onAccept: (FriendRequestWithSender) -> Unit,
    onDecline: (FriendRequestWithSender) -> Unit,
    onCancel: (FriendRequestWithReceiver) -> Unit,
    onDelete: (UserModel) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Phần lời mời kết bạn
        if (requests.isNotEmpty()) {
            item {
                Text("Lời mời kết bạn", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(requests) { requestWithSender ->
                UserItem(
                    name = requestWithSender.sender.displayName ?: "Người dùng ẩn danh",
                    actionButton = {
                        Row {
                            Button(
                                onClick = { onAccept(requestWithSender) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF198754))
                            ) { Text("Chấp nhận") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onDecline(requestWithSender) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545))
                            ) { Text("Từ chối") }
                        }
                    }
                )
            }
        }

        // Phần lời mời đã gửi
        if (sentRequests.isNotEmpty()) {
            item {
                Text("Lời mời đã gửi", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(sentRequests) { requestWithReceiver ->
                UserItem(
                    name = requestWithReceiver.receiver.displayName ?: "...",
                    actionButton = {
                        Button(
                            onClick = { onCancel(requestWithReceiver) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Hủy")
                        }
                    }
                )
            }
        }
        // Phần danh sách bạn bè
        item {
            Text("Danh sách bạn hiện có", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (friends.isEmpty()){
            item {
                Text(
                    text = "Chưa có người bạn nào. Hãy tìm kiếm và kết bạn nhé!",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(friends) { friend ->
                UserItem(
                    name = friend.displayName ?: "Người dùng ẩn danh",
                    actionButton = {
                        // Nút "Xóa"
                        Button(
                            onClick = { onDelete(friend) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545)),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("Xóa")
                        }
                    }
                )
            }
        }
    }
}

// Một Composable chung để hiển thị một hàng người dùng, tái sử dụng cho cả 3 danh sách
@Composable
fun UserItem(name: String, actionButton: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                painter = painterResource(id = R.drawable.img), // Thay bằng ảnh thật sau
                contentDescription = "Avatar",
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = name, fontSize = 16.sp, maxLines = 1)
        }
        Spacer(modifier = Modifier.width(8.dp))
        actionButton()
    }
}


// Composable để xem trước giao diện trong Android Studio
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddFriendScreenPreview() {
    MaterialTheme {
        // Preview sẽ không hoạt động hoàn chỉnh vì cần ViewModel,
        // nhưng vẫn có thể dùng để tinh chỉnh các Composable con.
        AddFriendScreen(onClose = {})
    }
}
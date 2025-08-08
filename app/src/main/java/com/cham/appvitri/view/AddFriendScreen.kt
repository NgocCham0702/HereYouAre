package com.cham.appvitri.view


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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cham.appvitri.R
// Composable chính cho toàn bộ màn hình
@Composable
fun AddFriendScreen() {
    // Sử dụng Scaffold để có cấu trúc TopBar và nội dung chuẩn
    Scaffold(
        topBar = {
            // Phần thanh tiêu đề màu xanh ở trên cùng
            TopBar(
                onClose = { /* Xử lý sự kiện đóng sau */ }
            )
        },
        // Màu nền cho phần nội dung chính
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        // Column để xếp các thành phần theo chiều dọc
        Column(
            modifier = Modifier
                .padding(paddingValues) // Áp dụng padding từ Scaffold
                .padding(horizontal = 16.dp) // Thêm padding ngang cho các thành phần bên trong
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Phần tìm kiếm
            SearchSection()

            Spacer(modifier = Modifier.height(24.dp))

            // Phần mã cá nhân
            PersonalCodeSection()

            Spacer(modifier = Modifier.height(24.dp))

            // Phần danh sách bạn bè
            FriendListSection()
        }
    }
}

// Composable cho thanh tiêu đề (Top Bar)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(onClose: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "Thêm bạn bè",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        actions = {
            // Nút "Đóng"
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Đóng")
            }
        },
        // Màu nền cho TopAppBar
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0D6EFD) // Màu xanh dương từ ảnh
        )
    )
}

// Composable cho khu vực tìm kiếm
@Composable
fun SearchSection() {
    // Trạng thái cho ô nhập liệu, tạm thời quản lý tại đây
    var searchQuery by remember { mutableStateOf("") }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text("Nhập tên, email, mã mời hoặc SĐT...") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(8.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = { /* Xử lý sự kiện tìm kiếm sau */ },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D6EFD))
    ) {
        Text("Tìm kiếm", fontSize = 16.sp, modifier = Modifier.padding(vertical = 6.dp))
    }
}

// Composable cho khu vực hiển thị mã cá nhân
@Composable
fun PersonalCodeSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally // Căn giữa các thành phần
    ) {
        Text("Mã cá nhân của bạn:", fontSize = 18.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text("cham-1234", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { /* Xử lý sự kiện sao chép sau */ },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF198754)) // Màu xanh lá
        ) {
            Text("Sao chép mã")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Cập nhật sau 1 ngày", fontSize = 12.sp, color = Color.Gray)
    }
}

// Composable cho toàn bộ danh sách bạn bè
@Composable
fun FriendListSection() {
    // Dữ liệu giả (hardcoded) để hiển thị danh sách
    val friends = listOf("Nguyễn Văn A", "Trần Thị B", "Lê Văn C")

    Text(
        "Danh sách bạn hiện có",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    // LazyColumn để hiển thị danh sách một cách hiệu quả
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(friends) { friendName ->
            FriendItem(
                name = friendName,
                onHide = { /* Xử lý ẩn bạn sau */ },
                onDelete = { /* Xử lý xóa bạn sau */ }
            )
        }
    }
}

// Composable cho một hàng (item) trong danh sách bạn bè
@Composable
fun FriendItem(name: String, onHide: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)) // Bo góc cho item
            .background(Color.White) // Nền trắng cho item
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon và chữ "Avatar"
        Icon(
            painter = painterResource(id = R.drawable.img), // Sử dụng icon đã tạo
            contentDescription = "Avatar",
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(4.dp))
        // Tên bạn bè
        Text(
            text = name,
            modifier = Modifier.weight(1f), // Chiếm hết không gian còn lại
            fontSize = 16.sp
        )

        // Nút "Ẩn"
        Button(
            onClick = onHide,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)), // Màu vàng
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Text("Ẩn", color = Color.Black)
        }
        Spacer(modifier = Modifier.width(8.dp))

        // Nút "Xóa"
        Button(
            onClick = onDelete,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545)), // Màu đỏ
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text("Xóa")
        }
    }
}

// Composable để xem trước giao diện trong Android Studio
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddFriendScreenPreview() {
    MaterialTheme { // Nên bọc trong Theme của bạn
        AddFriendScreen()
    }
}
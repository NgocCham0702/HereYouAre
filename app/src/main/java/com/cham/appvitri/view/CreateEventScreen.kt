package com.cham.appvitri.view

// File: view/CreateEventScreen.kt


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onNavigateBack: () -> Unit,
    onCreateEvent: (title: String, location: String, date: String, time: String) -> Unit
) {
    // States để lưu trữ dữ liệu người dùng nhập vào
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo sự kiện mới") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Cho phép cuộn nếu màn hình nhỏ
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Vui lòng điền thông tin sự kiện", style = MaterialTheme.typography.titleMedium)

            // Ô nhập Tên sự kiện
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tên sự kiện") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            // Ô nhập Địa điểm
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Địa điểm") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                trailingIcon = { // Nút chọn từ bản đồ
                    IconButton(onClick = { /* TODO: Mở bản đồ để chọn điểm */ }) {
                        Icon(Icons.Default.Map, contentDescription = "Chọn trên bản đồ")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Ô chọn Ngày
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Ngày diễn ra") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                trailingIcon = { // Nút mở lịch
                    IconButton(onClick = { /* TODO: Mở Date Picker Dialog */ }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Chọn ngày")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Ô chọn Thời gian
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Thời gian") },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                trailingIcon = { // Nút mở đồng hồ
                    IconButton(onClick = { /* TODO: Mở Time Picker Dialog */ }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Chọn giờ")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Tạo sự kiện
            Button(
                onClick = {
                    // TODO: Kiểm tra dữ liệu hợp lệ trước khi gọi callback
                    onCreateEvent(title, location, date, time)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("TẠO SỰ KIỆN")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateEventScreenPreview() {
    CreateEventScreen(onNavigateBack = {}, onCreateEvent = { _, _, _, _ -> })
}
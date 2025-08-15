package com.cham.appvitri.view

// File: view/CreateEventScreen.kt


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.viewModel.CreateEventViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: CreateEventViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    // States để lưu trữ dữ liệu người dùng nhập vào
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    val showDatePicker = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val friends by viewModel.friends.collectAsState()
    val selectedFriendUids by viewModel.selectedFriendUids.collectAsState()
    var showFriendPicker by remember { mutableStateOf(false) }

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedDateMillis != null) {
                            // Chuyển đổi mili-giây sang định dạng dd/MM/yyyy
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            date = sdf.format(Date(selectedDateMillis))
                        }
                        showDatePicker.value = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    LaunchedEffect(uiState) {
        if (uiState.eventCreationSuccess) {
            // Nếu tạo sự kiện thành công, tự động quay lại
            onNavigateBack()
        }
        // Có thể thêm Snackbar để hiển thị errorMessage ở đây
    }
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
                label = { Text("Ngày (dd/MM/yyyy)") }, // <<< Gợi ý định dạng
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                trailingIcon = { // Nút mở lịch
                    IconButton(onClick = { showDatePicker.value = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Chọn ngày")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            val showTimePicker = remember { mutableStateOf(false) }
            val timePickerState = rememberTimePickerState()
            if (showTimePicker.value) {
                // Cần một Composable bọc bên ngoài để TimePickerDialog hiển thị đúng
                AlertDialog(onDismissRequest = { showTimePicker.value = false }) {
                    Column(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimePicker(state = timePickerState)
                        Row {
                            TextButton(onClick = { showTimePicker.value = false }) { Text("Hủy") }
                            TextButton(
                                onClick = {
                                    // Định dạng giờ và phút
                                    time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                                    showTimePicker.value = false
                                }
                            ) { Text("OK") }
                        }
                    }
                }
            }
            // Ô chọn Thời gian
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Thời gian (HH:mm)") }, // <<< Gợi ý định dạng
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                trailingIcon = { // Nút mở đồng hồ
                    IconButton(onClick = { showTimePicker.value = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Chọn giờ")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
// --- THÊM PHẦN MỜI BẠN BÈ ---
            Text("Mời bạn bè:", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showFriendPicker = true }) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Mời bạn")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chọn người tham gia (${selectedFriendUids.size})")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Nút Tạo sự kiện
            Button(
                onClick = {
                    // TODO: Kiểm tra dữ liệu hợp lệ trước khi gọi callback
                    viewModel.createEvent(title, location, date, time)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            else {Text("TẠO SỰ KIỆN")}
            }
            // --- DIALOG CHỌN BẠN BÈ (TÁI SỬ DỤNG TỪ CREATEGROUPSCREEN) ---
            if (showFriendPicker) {
                AlertDialog(
                    onDismissRequest = { showFriendPicker = false },
                    title = { Text("Chọn người tham gia") },
                    text = {
                        LazyColumn {
                            items(friends) { friend ->
                                // Giả sử bạn đã có Composable này từ màn hình tạo nhóm
                                FriendSelectItem(
                                    friend = friend,
                                    isSelected = selectedFriendUids.contains(friend.uid),
                                    onSelectionChange = { viewModel.toggleFriendSelection(friend.uid) }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFriendPicker = false }) { Text("Xong") }
                    }
                )
            }

        }

    }
}

@Preview(showBackground = true)
@Composable
fun CreateEventScreenPreview() {
    CreateEventScreen( onNavigateBack = { /*TODO*/ })
}
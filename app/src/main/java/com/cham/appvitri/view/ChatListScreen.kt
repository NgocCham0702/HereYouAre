package com.cham.appvitri.view

// File: view/chat/ChatListScreen.kt

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cham.appvitri.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cham.appvitri.viewModel.ChatListViewModel

data class ChatPreview(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val avatarResId: Int,
    val isGroup: Boolean
)
// --- Giao diện ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(onNavigateBack: () -> Unit,
                   onChatItemClicked: (String) -> Unit,
                   onNavigateToCreateGroup: () -> Unit,
                   viewModel: ChatListViewModel = viewModel()) {
    val chatList by viewModel.chatList.collectAsState()
    // State để mở/đóng dialog
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    // State để lưu trữ thông tin của chat đang được chọn để xóa
    var chatToDelete by remember { mutableStateOf<ChatPreview?>(null) }
    if (showDeleteConfirmationDialog && chatToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                // Khi người dùng bấm ra ngoài hoặc nút back, đóng dialog
                showDeleteConfirmationDialog = false
            },
            title = {
                Text(text = "Xác nhận xóa")
            },
            text = {
                Text("Bạn có chắc chắn muốn xóa cuộc trò chuyện với \"${chatToDelete!!.name}\"? Hành động này không thể hoàn tác.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Người dùng đồng ý -> gọi ViewModel để xóa
                        viewModel.deleteChat(chatToDelete!!.id)
                        // Đóng dialog
                        showDeleteConfirmationDialog = false
                    }
                ) {
                    Text("Đồng ý")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        // Người dùng hủy -> đóng dialog
                        showDeleteConfirmationDialog = false
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateGroup) {
                Icon(Icons.Default.Add, contentDescription = "Tạo cuộc trò chuyện mới")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding)
        ) {
            items(chatList) { chat ->
                ChatItemRow(chat = chat, onClick = {onChatItemClicked(chat.id)
                    // TODO: Điều hướng đến màn hình ChatDetailScreen với chat.id
                    println("Navigate to chat with ID: ${chat.id}")

                },
                    onDelete = {
//                        Log.d("DELETE_DEBUG", "Bấm nút xóa cho Chat ID: ${chat.id}")
//                        viewModel.deleteChat(chat.id)
                           chatToDelete = chat
                           showDeleteConfirmationDialog = true})
                Divider(modifier = Modifier.padding(start = 88.dp)) // Thụt lề cho đường kẻ
            }
        }
    }
}

@Composable
fun ChatItemRow(chat: ChatPreview, onClick: () -> Unit,onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = chat.avatarResId),
            contentDescription = chat.name,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = chat.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = chat.lastMessage,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Hiển thị "..." nếu tin nhắn quá dài
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = chat.timestamp,
            fontSize = 12.sp,
            color = Color.Gray
        )
        // Thêm nút xóa
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Xóa")
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun ChatListScreenPreview() {
//    ChatListScreen()
//}
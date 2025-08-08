package com.cham.appvitri.view

// File: view/chat/ChatDetailScreen.kt

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import com.cham.appvitri.R
import kotlinx.coroutines.launch

// --- Dữ liệu giả (Mock Data) ---
data class Message(
    val id: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean // Quan trọng: để xác định tin nhắn của mình hay của bạn
)

val mockMessages = listOf(
    Message("1", "Chào bạn, khỏe không?", "10:25 SA", false),
    Message("2", "Chào, mình khỏe. Còn bạn?", "10:26 SA", true),
    Message("3", "Mình cũng ổn. Tối nay đi xem phim không?", "10:26 SA", false),
    Message("4", "Ok bạn, hẹn gặp lại nhé!", "10:27 SA", true),
    Message("5", "Phim gì thế?", "10:28 SA", true),
    Message("6", "Phim mới của Marvel đó. Nghe nói hay lắm!", "10:29 SA", false),
    Message("7", "Wow, tuyệt vời! Mấy giờ thế?", "10:30 SA", true),
)

// --- Giao diện ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatName: String,
    chatAvatarResId: Int,
    onNavigateBack: () -> Unit // Hàm để quay lại màn hình trước
) {
    // State để quản lý văn bản đang nhập
    var messageText by remember { mutableStateOf("") }
    // State để quản lý cuộn của danh sách tin nhắn
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ChatDetailTopBar(
                name = chatName,
                avatarResId = chatAvatarResId,
                onBackClicked = onNavigateBack
            )
        },
        bottomBar = {
            MessageInputBar(
                text = messageText,
                onTextChange = { messageText = it },
                onSendClicked = {
                    if (messageText.isNotBlank()) {
                        // TODO: Gửi tin nhắn thật
                        println("Sending message: $messageText")
                        messageText = "" // Xóa text sau khi gửi
                        // Tự động cuộn xuống tin nhắn mới nhất
                        coroutineScope.launch {
                            listState.animateScrollToItem(mockMessages.size - 1)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(mockMessages) { message ->
                MessageBubble(message = message)
            }
        }
    }

    // Tự động cuộn xuống dưới cùng khi màn hình được hiển thị lần đầu
    LaunchedEffect(Unit) {
        listState.scrollToItem(mockMessages.size - 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailTopBar(name: String, avatarResId: Int, onBackClicked: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = avatarResId),
                    contentDescription = name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = name, fontWeight = FontWeight.SemiBold)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
fun MessageBubble(message: Message) {
    // Sắp xếp tin nhắn của bạn sang phải, của người khác sang trái
    val horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            // Đổi màu cho tin nhắn của bạn và của người khác
            color = if (message.isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f, fill = false) // Để bong bóng chat co giãn theo nội dung
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = if (message.isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(
                onClick = onSendClicked,
                enabled = text.isNotBlank() // Chỉ bật nút gửi khi có text
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ChatDetailScreenPreview() {
    // Gọi hàm với dữ liệu mẫu để xem trước
    ChatDetailScreen(
        chatName = "An Nguyên",
        chatAvatarResId = R.drawable.img_14,
        onNavigateBack = {}
    )
}
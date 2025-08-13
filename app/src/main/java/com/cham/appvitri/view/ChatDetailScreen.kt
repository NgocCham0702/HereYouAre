package com.cham.appvitri.view

// File: view/chat/ChatDetailScreen.kt

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cham.appvitri.viewModel.ChatDetailViewModel
import kotlin.collections.isNotEmpty
import androidx.compose.foundation.lazy.items // <<< THÊM DÒNG NÀY
import com.cham.appvitri.model.Message
import com.cham.appvitri.R
// --- Dữ liệu giả (Mock Data) ---
// --- Giao diện ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            ChatDetailTopBar(
                name = uiState.chatName,
                avatarResId = uiState.chatAvatarResId,
                onBackClicked = onNavigateBack
            )
        },
        bottomBar = {
            MessageInputBar(
                text = messageText,
                onTextChange = { messageText = it },
                onSendClicked = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = "" // Xóa text sau khi gửi
                    }
                },
                modifier = Modifier.padding(bottom = 40.dp)
            )
        }
    ) { innerPadding ->

        // --- BẮT ĐẦU SỬA ĐỔI TẠI ĐÂY ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // 1. Trạng thái ĐANG TẢI
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // 2. Trạng thái TẢI XONG & RỖNG (thêm cái này)
                uiState.messages.isEmpty() -> {
                    Text(
                        text = "Chưa có tin nhắn nào.\nHãy gửi lời chào!",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // 3. Trạng thái TẢI XONG & CÓ DỮ LIỆU
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 3.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.messages) { message -> // Dòng này sẽ hết lỗi
                            MessageBubble(message = message,
                                isGroupChat = uiState.isGroupChat)
                        }
                    }
                }
            }
        }
        // --- KẾT THÚC SỬA ĐỔI ---
    }
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.size - 1)
        }
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
fun MessageBubble(message: Message, isGroupChat: Boolean) {
    val horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        // Cột này chứa avatar và bong bóng chat
        Row(verticalAlignment = Alignment.Bottom) {
            // Hiển thị avatar cho người khác trong nhóm
            if (!message.isFromMe && isGroupChat) {
                Image(
                    painter = painterResource(id = message.senderAvatarResId),
                    contentDescription = message.senderName,
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Cột này chứa tên (nếu có) và bong bóng chat
            Column {
                // Hiển thị tên cho người khác trong nhóm
                if (!message.isFromMe && isGroupChat) {
                    Text(
                        text = message.senderName ?: "Người dùng",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
                    )
                }

                // --- BONG BÓNG CHAT LUÔN HIỂN THỊ NỘI DUNG ---
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (message.isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = message.text, // <<< LUÔN LÀ message.text
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (message.isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
@Composable
fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClicked: () -> Unit,
    modifier: Modifier = Modifier) {
    Surface(
        tonalElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
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
//-------------------------------------------

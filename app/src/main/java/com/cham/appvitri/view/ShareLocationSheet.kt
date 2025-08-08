package com.cham.appvitri.view

// File: view/ShareLocationSheet.kt

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.cham.appvitri.R

// --- Data classes để quản lý dữ liệu ---
data class ShareOption(val iconResId: Int, val name: String)
data class TimeOption(val label: String, val durationMinutes: Int)

@Composable
fun ShareLocationSheet(
    onDismiss: () -> Unit // Hàm callback để đóng BottomSheet
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // --- State Management ---
    // Thêm trạng thái để theo dõi việc chia sẻ
    var isSharing by remember { mutableStateOf(false) }

    val timeOptions = listOf(
        TimeOption("15 phút", 15),
        TimeOption("1 giờ", 60),
        TimeOption("4 giờ", 240),
        TimeOption("24 giờ", 24 * 60)
    )
    var selectedTime by remember { mutableStateOf(timeOptions[1]) }

    val shareLink = "https://myapp.dev/share/aB1cE2"
    val shareCode = "X7Y-3Z1"
    val socialApps = listOf(
        ShareOption(R.drawable.img_9, "Messenger"),
        ShareOption(R.drawable.img_10, "Zalo"),
        ShareOption(R.drawable.img_11, "Tin nhắn"),
        ShareOption(R.drawable.img_12, "Thêm")
    )

    // --- Định nghĩa màu sắc cho Button ---
    val shareButtonColor = Color(0xFF4CAF50) // Màu xanh lá
    val stopButtonColor = Color(0xFFF44336)  // Màu đỏ

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // --- 1. Tiêu đề và Nút Back ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Chia sẻ vị trí của bạn",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }
        Divider()

        Column(modifier = Modifier.padding(16.dp)) {
            // --- 2. Tùy chọn Thời gian ---
            Text("Chia sẻ trong:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timeOptions.forEach { option ->
                    FilterChip(
                        selected = (option == selectedTime),
                        onClick = { selectedTime = option },
                        label = { Text(option.label) },
                        enabled = !isSharing, // Vô hiệu hóa khi đang chia sẻ
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. Liên kết và Mã chia sẻ ---
            Text("Chia sẻ bằng liên kết hoặc mã", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(shareLink, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(shareLink)) },
                        enabled = !isSharing // Vô hiệu hóa khi đang chia sẻ
                    ) {
                        Icon(painterResource(id = R.drawable.img_13), contentDescription = "Copy Link")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(shareCode, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(shareCode)) },
                        enabled = !isSharing // Vô hiệu hóa khi đang chia sẻ
                    ) {
                        Icon(painterResource(id = R.drawable.img_13), contentDescription = "Copy Code")
                    }
                }
            }
            Text("Mã sẽ được làm mới sau 24 giờ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
// --- 5. Nút Hành động (Mới) ---
            if (isSharing) {
                // Nút Dừng chia sẻ (màu đỏ)
                Button(
                    onClick = { isSharing = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = stopButtonColor)
                ) {
                    Text("Dừng chia sẻ", color = Color.White)
                }
            } else {
                // Nút Bắt đầu chia sẻ (màu xanh lá)
                Button(
                    onClick = { isSharing = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = shareButtonColor)
                ) {
                    Text("Bắt đầu chia sẻ", color = Color.White)
                }
            }
            // --- 4. Chia sẻ nhanh qua MXH ---
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items(socialApps) { app ->
                    SocialShareItem(
                        iconResId = app.iconResId,
                        name = app.name,
                        // Vô hiệu hóa khi đang chia sẻ
                        onClick = { if (!isSharing) { /* TODO */ } }
                    )
                }
            }
        }
    }
}

@Composable
fun SocialShareItem(@DrawableRes iconResId: Int, name: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = name,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, fontSize = 12.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun ShareLocationSheetPreview() {
    ShareLocationSheet(onDismiss = {})
}
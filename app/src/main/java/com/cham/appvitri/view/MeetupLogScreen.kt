package com.cham.appvitri.view

// File: view/meetup/MeetupLogScreen.kt
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cham.appvitri.R

// --- Dữ liệu giả (Mock Data) ---
data class Meetup(
    val id: String,
    val date: String,
    val participants: List<Participant>,
    val durationMinutes: Int,
    val address: String
)

data class Participant(val name: String, val avatarResId: Int)

val mockMeetupLog = listOf(
    Meetup("1", "Thứ Hai, 25 tháng 12",
        participants = listOf(
            Participant("An Nguyên", R.drawable.img),
            Participant("Lê Minh", R.drawable.img)
        ),
        durationMinutes = 45,
        address = "Highlands Coffee, Lê Lợi"
    ),
    Meetup("2", "Thứ Bảy, 23 tháng 12",
        participants = listOf(
            Participant("An Nguyên", R.drawable.img),
            Participant("Lê Minh", R.drawable.img),
            Participant("Bạn", R.drawable.img) // Thêm avatar của chính bạn
        ),
        durationMinutes = 125,
        address = "Lotte Cinema Gò Vấp"
    )
)

// --- Giao diện ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetupLogScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhật ký Gặp gỡ") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(mockMeetupLog) { meetup ->
                MeetupItemCard(meetup = meetup)
            }
        }
    }
}

@Composable
fun MeetupItemCard(meetup: Meetup) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tiêu đề ngày tháng
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = "Date", tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = meetup.date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            // Mô tả cuộc gặp gỡ
            Text(
                buildAnnotatedString {
                    append("Bạn đã có một cuộc gặp gỡ với ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(meetup.participants.first().name)
                    }
                    if (meetup.participants.size > 2) {
                        append(" và ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("${meetup.participants.size - 2} người khác")
                        }
                    } else if (meetup.participants.size == 2 && meetup.participants.any { it.name != "Bạn" }) {
                        // xử lý trường hợp chỉ có 2 người
                    }
                }
            )

            // Hiển thị chồng các avatar
            OverlappingAvatars(participants = meetup.participants)

            // Thông tin chi tiết
            InfoRow(icon = { Icon(Icons.Default.Timer, contentDescription = "Duration", tint = Color.Gray) },
                text = "Kéo dài ${meetup.durationMinutes} phút")
            InfoRow(icon = { Icon(Icons.Default.Place, contentDescription = "Address", tint = Color.Gray) },
                text = "Tại ${meetup.address}")
        }
    }
}

@Composable
fun OverlappingAvatars(participants: List<Participant>) {
    Box(modifier = Modifier.height(32.dp)) {
        participants.take(5).forEachIndexed { index, participant -> // Chỉ hiển thị tối đa 5 avatar
            Image(
                painter = painterResource(id = participant.avatarResId),
                contentDescription = participant.name,
                modifier = Modifier
                    .padding(start = (index * 20).dp) // Tạo hiệu ứng xếp chồng
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun InfoRow(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 14.sp, color = Color.Gray)
    }
}


@Preview(showBackground = true)
@Composable
fun MeetupLogScreenPreview() {
    MeetupLogScreen()
}
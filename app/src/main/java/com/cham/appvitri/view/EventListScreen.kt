package com.cham.appvitri.view

// File: view/event/EventListScreen.kt


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Dữ liệu giả (Mock Data) ---
data class Event(
    val id: String,
    val title: String,
    val address: String,
    val date: String,
    val time: String,
    val isUpcoming: Boolean
)

val mockEventList = listOf(
    Event("1", "Xem phim cuối tuần", "CGV Vincom Gò Vấp", "Thứ Bảy, 25/12", "19:30", true),
    Event("2", "Ăn tối cùng nhóm bạn", "Kichi-Kichi Phan Văn Trị", "Thứ Sáu, 24/12", "18:00", true),
    Event("3", "Chuyến đi Vũng Tàu", "Bãi Sau, Vũng Tàu", "18/12 - 19/12", "Cả ngày", false),
    Event("4", "Sinh nhật An", "Nhà hàng The Pizza Company", "15/12", "19:00", false)
)

// --- Giao diện ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sự kiện của bạn") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Mở màn hình tạo sự kiện */ }) {
                Icon(Icons.Default.Add, contentDescription = "Tạo sự kiện mới")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nhóm các sự kiện sắp diễn ra
            item {
                Text(
                    "Sắp diễn ra",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(mockEventList.filter { it.isUpcoming }) { event ->
                EventItemCard(event = event)
            }

            // Nhóm các sự kiện đã qua
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Đã qua",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(mockEventList.filter { !it.isUpcoming }) { event ->
                EventItemCard(event = event, isPastEvent = true)
            }
        }
    }
}

@Composable
fun EventItemCard(event: Event, isPastEvent: Boolean = false) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            // Làm mờ các sự kiện đã qua
            containerColor = if (isPastEvent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPastEvent) Color.Gray else MaterialTheme.colorScheme.onSurface
            )

            EventInfoRow(
                icon = Icons.Default.Place,
                text = event.address,
                isPastEvent = isPastEvent
            )
            EventInfoRow(
                icon = Icons.Default.CalendarToday,
                text = event.date,
                isPastEvent = isPastEvent
            )
            EventInfoRow(
                icon = Icons.Default.AccessTime,
                text = event.time,
                isPastEvent = isPastEvent
            )
        }
    }
}

@Composable
fun EventInfoRow(icon: ImageVector, text: String, isPastEvent: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPastEvent) Color.Gray.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isPastEvent) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EventListScreenPreview() {
    EventListScreen()
}
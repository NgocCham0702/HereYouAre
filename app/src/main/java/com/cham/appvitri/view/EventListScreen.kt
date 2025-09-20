package com.cham.appvitri.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cham.appvitri.repository.Event // <<< SỬ DỤNG MODEL EVENT MỚI
import com.cham.appvitri.viewModel.EventListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onNavigateToCreateEvent: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val viewModel: EventListViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sự kiện của bạn") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Dùng icon AutoMirrored
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateEvent) {
                Icon(Icons.Default.Add, contentDescription = "Tạo sự kiện mới")
            }
        }
    ) { innerPadding ->
        // --- THÊM XỬ LÝ TRẠNG THÁI RỖNG ---
        if (uiState.upcomingEvents.isEmpty() && uiState.pastEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Chưa có sự kiện nào. Hãy tạo một cái mới!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.upcomingEvents.isNotEmpty()) {
                    item {
                        Text(
                            "Sắp diễn ra",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(uiState.upcomingEvents) { event ->
                        EventItemCard(event = event, isPastEvent = false)
                    }
                }

                if (uiState.pastEvents.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Đã qua",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(uiState.pastEvents) { event ->
                        EventItemCard(event = event, isPastEvent = true)
                    }
                }
            }
        }
    }
}

@Composable
fun EventItemCard(event: Event, isPastEvent: Boolean) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
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
                text = event.address, // <<< SỬA: DÙNG TRỰC TIẾP TỪ MODEL
                isPastEvent = isPastEvent
            )
            EventInfoRow(
                icon = Icons.Default.CalendarToday,
                text = event.dateString, // <<< SỬA: DÙNG COMPUTED PROPERTY
                isPastEvent = isPastEvent
            )
            EventInfoRow(
                icon = Icons.Default.AccessTime,
                text = event.timeString, // <<< SỬA: DÙNG COMPUTED PROPERTY
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
// Tạo file viewModel/EventListViewModel.kt

package com.cham.appvitri.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.Event
import com.cham.appvitri.repository.EventRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// UI State để chứa danh sách sự kiện đã được phân loại
data class EventListUiState(
    val upcomingEvents: List<Event> = emptyList(),
    val pastEvents: List<Event> = emptyList(),
    val isLoading: Boolean = true
)

class EventListViewModel : ViewModel() {
    private val eventRepository = EventRepository()
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(EventListUiState())
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        val userId = authRepository.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            eventRepository.getEventsFlow(userId).collect { eventModels ->
                // Lấy thời gian hiện tại để so sánh
                val now = Timestamp.now()
                
                // Phân loại sự kiện
                val upcoming = mutableListOf<Event>()
                val past = mutableListOf<Event>()

                eventModels.forEach { model ->
                    // Chuyển đổi EventModel (backend) thành Event (UI)
                    val uiEvent = Event(
                        id = model.id,
                        title = model.title ?: "Không có tiêu đề",
                        address = model.address ?: "Không rõ địa điểm",
                        // Định dạng lại Timestamp thành String dễ đọc
                        date = formatDate(model.eventTimestamp),
                        time = formatTime(model.eventTimestamp),
                        isUpcoming = model.eventTimestamp?.let { it > now } ?: false
                    )

                    if (uiEvent.isUpcoming) {
                        upcoming.add(uiEvent)
                    } else {
                        past.add(uiEvent)
                    }
                }
                
                // Cập nhật state cho UI
                _uiState.update { 
                    it.copy(
                        upcomingEvents = upcoming,
                        pastEvents = past,
                        isLoading = false
                    ) 
                }
            }
        }
    }

    // Các hàm tiện ích để định dạng ngày giờ
    private fun formatDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "Không rõ ngày"
        val sdf = SimpleDateFormat("EEEE, dd/MM", Locale("vi", "VN"))
        return sdf.format(timestamp.toDate())
    }

    private fun formatTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "Không rõ giờ"
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
}
package com.cham.appvitri.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.Event
import com.cham.appvitri.repository.EventRepository
import com.cham.appvitri.utils.scheduleEventReminder // <<< BƯỚC 1: IMPORT HÀM LẬP LỊCH
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UI State đã được đơn giản hóa, không cần isLoading nữa vì Flow xử lý trạng thái ban đầu
data class EventListUiState(
    val upcomingEvents: List<Event> = emptyList(),
    val pastEvents: List<Event> = emptyList()
)

// <<< BƯỚC 2: THAY ĐỔI TỪ ViewModel SANG AndroidViewModel
class EventListViewModel(application: Application) : AndroidViewModel(application) {
    private val eventRepository = EventRepository()
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(EventListUiState())
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            // Xử lý trường hợp người dùng chưa đăng nhập
            _uiState.update { it.copy(upcomingEvents = emptyList(), pastEvents = emptyList()) }
            return
        }

        viewModelScope.launch {
            eventRepository.getUserEventsFlow(userId)
                .catch { exception ->
                    // Xử lý lỗi nếu có
                    // Ví dụ: _uiState.update { it.copy(errorMessage = "Lỗi tải sự kiện") }
                }
                .collect { allEvents ->
                    val now = Timestamp.now()

                    // Phân loại sự kiện trực tiếp từ danh sách nhận được
                    val upcoming = allEvents.filter { it.eventTimestamp >= now }
                    val past = allEvents.filter { it.eventTimestamp < now }

                    // Cập nhật state cho UI
                    _uiState.update {
                        it.copy(
                            // Sắp xếp sự kiện sắp tới theo thời gian gần nhất trước
                            upcomingEvents = upcoming.sortedBy { e -> e.eventTimestamp },
                            // Sự kiện đã qua vẫn giữ nguyên thứ tự mới nhất trước
                            pastEvents = past
                        )
                    }

                    // --- PHẦN LOGIC MỚI QUAN TRỌNG NHẤT ---
                    // <<< BƯỚC 3: GỌI HÀM ĐỂ LẬP LỊCH THÔNG BÁO
                    scheduleRemindersForUpcomingEvents(upcoming)
                }
        }
    }

    /**
     * Hàm này nhận danh sách các sự kiện sắp diễn ra và đặt lịch thông báo
     * cho từng sự kiện bằng WorkManager.
     */
    private fun scheduleRemindersForUpcomingEvents(events: List<Event>) {
        // Lấy context từ Application mà AndroidViewModel cung cấp
        val context = getApplication<Application>().applicationContext
        events.forEach { event ->
            // Gọi hàm tiện ích mà chúng ta sẽ tạo ở bước tiếp theo
            scheduleEventReminder(context, event)
        }
    }
}
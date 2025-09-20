package com.cham.appvitri.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data Model CHÍNH cho một sự kiện.
 * Model này được dùng trong toàn bộ ứng dụng, từ Firestore đến UI.
 */
data class Event(
    // @get:Exclude để Firebase không cố gắng ghi trường 'id' này vào document,
    // vì id chính là tên của document.
    @get:Exclude var id: String = "",
    val title: String = "",
    val address: String = "",
    val eventTimestamp: Timestamp = Timestamp.now(), // Nguồn dữ liệu thời gian duy nhất và chính xác
    val creatorUid: String = "",
    val participantUids: List<String> = emptyList(),
    val notificationSent: Boolean = false // Dành cho logic Cloud Function sau này
) {
    // Computed property để dễ dàng hiển thị ngày tháng trên UI
    @get:Exclude
    val dateString: String
        get() {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return sdf.format(eventTimestamp.toDate())
        }

    // Computed property để dễ dàng hiển thị giờ trên UI
    @get:Exclude
    val timeString: String
        get() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(eventTimestamp.toDate())
        }
}


class EventRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val eventsCollection = firestore.collection("events")

    /**
     * Lắng nghe danh sách các sự kiện mà người dùng tham gia.
     * Sắp xếp theo thời gian sự kiện, sự kiện sắp tới sẽ ở trên cùng.
     */
    fun getUserEventsFlow(userId: String): Flow<List<Event>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val query = eventsCollection
            .whereArrayContains("participantUids", userId)
            // Sắp xếp theo thứ tự giảm dần, sự kiện trong tương lai xa nhất sẽ ở trên cùng
            .orderBy("eventTimestamp", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val events = snapshot?.documents?.mapNotNull { doc ->
                // Chuyển đổi document thành object Event và gán ID cho nó
                doc.toObject(Event::class.java)?.apply { id = doc.id }
            } ?: emptyList()

            trySend(events)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Tạo một sự kiện mới trong Firestore.
     */
    suspend fun createEvent(
        creatorUid: String,
        title: String,
        address: String,
        eventTimestamp: Timestamp,
        invitedFriendUids: List<String>
    ): Result<String> { // Trả về ID của sự kiện mới
        return try {
            // Người tạo cũng là một người tham gia
            val participantUids = (invitedFriendUids + creatorUid).distinct()

            val newEvent = Event(
                creatorUid = creatorUid,
                participantUids = participantUids,
                title = title,
                address = address,
                eventTimestamp = eventTimestamp,
                notificationSent = false // Mặc định là chưa gửi thông báo
            )

            // Thêm sự kiện mới vào collection, Firebase sẽ tự tạo ID
            val docRef = eventsCollection.add(newEvent).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
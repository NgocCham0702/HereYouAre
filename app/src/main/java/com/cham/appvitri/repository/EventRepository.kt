// Tạo file repository/EventRepository.kt

package com.cham.appvitri.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Event(
    val id: String,
    val title: String,
    val address: String,
    val date: String,
    val time: String,
    val isUpcoming: Boolean
)
// Data model khớp với cấu trúc trên Firestore
data class EventModel(
    @get:Exclude var id: String = "",
    val title: String? = null,
    val address: String? = null,
    val eventTimestamp: Timestamp? = null,
    val creatorUid: String? = null,
    val participantUids: List<String> = emptyList()
)

class EventRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val eventsCollection = firestore.collection("events")

    /**
     * Lắng nghe danh sách các sự kiện mà người dùng tham gia.
     * Sắp xếp theo thời gian sự kiện, sự kiện mới nhất/sắp tới sẽ ở trên cùng.
     */
    fun getEventsFlow(userId: String): Flow<List<EventModel>> = callbackFlow {
        val query = eventsCollection
            .whereArrayContains("participantUids", userId)
            .orderBy("eventTimestamp", Query.Direction.DESCENDING) // Sắp xếp giảm dần

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val events = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(EventModel::class.java)?.apply { id = doc.id }
            } ?: emptyList()
            trySend(events)
        }
        awaitClose { listener.remove() }
    }
    
    // (Sau này bạn sẽ thêm các hàm như createEvent, updateEvent,...)
    suspend fun createEvent(
        creatorUid: String,
        title: String,
        address: String,
        eventTimestamp: Timestamp,
        invitedFriendUids: List<String>
    ): Result<String> { // Trả về ID của sự kiện mới
        return try {
            // Người tạo mặc định cũng là một người tham gia
            // Gộp người tạo và bạn bè được mời vào danh sách tham gia
            val participantUids = (invitedFriendUids + creatorUid).distinct()

            val newEvent = EventModel(
                creatorUid = creatorUid,
                participantUids = participantUids,
                title = title,
                address = address,
                eventTimestamp = eventTimestamp
            )

            val docRef = eventsCollection.add(newEvent).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package com.cham.appvitri.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
data class SosSession(
    @get:Exclude var id: String = "",
    val requestingUserId: String? = null,
    val requestingUserName: String? = null,
    val requestingUserAvatarUrl: String? = null,
    val notifiedUserIds: List<String> = emptyList(),
    val lastKnownLocation: GeoPoint? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val status: String? = null
)
class SOSRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val sosCollection = firestore.collection("sos_sessions")
    private val userRepository = UserRepository() // Giả sử đã có
    private val authRepository = AuthRepository() // Giả sử đã có

    // Bắt đầu một phiên SOS
    suspend fun startSosSession(friendIds: List<String>, currentLocation: GeoPoint): Result<String> {
        return try {
            val currentUser = userRepository.getUserProfileOnce(authRepository.getCurrentUserId()!!).getOrNull()
            if (currentUser == null) return Result.failure(Exception("Không tìm thấy người dùng hiện tại"))

            val newSession = hashMapOf(
                "requestingUserId" to currentUser.uid,
                "requestingUserName" to currentUser.displayName,
                "requestingUserAvatarUrl" to currentUser.profilePictureUrl,
                "notifiedUserIds" to friendIds,
                "lastKnownLocation" to currentLocation,
                "createdAt" to FieldValue.serverTimestamp(),
                "status" to "active"
            )

            val docRef = sosCollection.add(newSession).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Hủy phiên SOS
    suspend fun cancelSosSession(sessionId: String): Result<Unit> {
        return try {
            sosCollection.document(sessionId).update("status", "cancelled").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    fun getIncomingSosFlow(myUserId: String): Flow<List<SosSession>> = callbackFlow {
        val query = sosCollection
            .whereArrayContains("notifiedUserIds", myUserId)
            .whereEqualTo("status", "active") // Chỉ lấy các phiên đang hoạt động

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val sessions = snapshot?.documents?.mapNotNull { doc ->
                // Sử dụng data class SosSession để chuyển đổi dữ liệu
                doc.toObject(SosSession::class.java)?.apply { id = doc.id }
            } ?: emptyList()

            // Gửi danh sách các phiên SOS đang hoạt động vào Flow
            trySend(sessions)
        }

        // Khi Flow bị hủy, gỡ bỏ listener để tránh rò rỉ bộ nhớ
        awaitClose { listener.remove() }
    }
}
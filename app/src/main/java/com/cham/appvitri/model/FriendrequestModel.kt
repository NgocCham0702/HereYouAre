package com.cham.appvitri.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

// Các trạng thái có thể có của một lời mời kết bạn
enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

/**
 * Model đại diện cho một lời mời kết bạn trong collection 'friend_requests'.
 */
data class FriendRequestModel(
    @get:com.google.firebase.firestore.Exclude
    @set:com.google.firebase.firestore.Exclude
    // ID của document này, có thể dùng để tham chiếu nhanh
    var requestId: String = "",

    // UID của người gửi lời mời
    var fromUid: String = "",

    // UID của người nhận lời mời
    var toUid: String = "",

    /**
     * Trạng thái của lời mời.
     * Nên sử dụng các hằng số từ FriendRequestStatus (ví dụ: "pending").
     */
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val fromName: String? = null,
    val toName: String? = null,
    // Dấu thời gian tự động của server khi lời mời được tạo
    @ServerTimestamp
    var createdAt: Timestamp? = null
)
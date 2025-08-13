package com.cham.appvitri.model

import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp
data class Message(
    val id: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val senderName: String?,
    val senderAvatarResId: Int
)
data class MessageModel(
    @get:com.google.firebase.firestore.Exclude
    var id: String = "",

    val senderId: String = "",
    val text: String = "",
    val senderName: String? = null,
    val senderAvatarUrl: String? = null,
    @ServerTimestamp
    val timestamp: Timestamp? = null
)

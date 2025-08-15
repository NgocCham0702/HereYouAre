package com.cham.appvitri.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp


data class ChatModel(
    @get:Exclude // Sửa lại import cho ngắn gọn
    var id: String = "",
    val participants: List<String> = emptyList(),
    val deletedBy: List<String> = emptyList(),
    val lastMessage: String? = null,
    @ServerTimestamp
    val lastMessageTimestamp: Timestamp? = null,
    // --- SỬA Ở ĐÂY ---
    @PropertyName("group") // Ánh xạ getter của isGroup với trường "group"
    @JvmField
    //@set:PropertyName("group") // Ánh xạ setter của isGroup với trường "group"
    val isGroup: Boolean = false,
    // -----------------
    // Dành cho chat nhóm (giữ nguyên)
    val groupName: String? = null,
    val groupAvatarUrl: String? = null
)

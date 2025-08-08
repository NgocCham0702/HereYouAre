package com.cham.appvitri.model

import com.google.firebase.Timestamp

//chứa cấu trúc dữ liệu người dùng
data class UserModel(
    val uid:String = "",
    val displayName:String? = null,
    val email:String? = null,
    val phoneNumber:String? = null,
    val profilePictureUrl:String? = "null",
    val bio:String?="",
    val authProviders: List<String> = emptyList(),
    val createdAt:Timestamp= Timestamp.now(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastUpdated: Timestamp = Timestamp.now()
)
//package com.cham.appvitri.model
//
//import com.google.firebase.Timestamp
//
////chứa cấu trúc dữ liệu người dùng
//data class UserModel(
//    val uid:String = "",
//    val displayName:String? = null,
//    val email:String? = null,
//    val phoneNumber:String? = null,
//    val profilePictureUrl:String? = "null",
//    val bio:String?="",
//    val authProviders: List<String> = emptyList(),
//    val createdAt:Timestamp= Timestamp.now(),
//    val latitude: Double? = null,
//    val longitude: Double? = null,
//    val lastUpdated: Timestamp = Timestamp.now()
//)
// file: com/cham/appvitri/model/UserModel.kt
package com.cham.appvitri.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

// QUAN TRỌNG: Các trường phải là var và có giá trị mặc định
data class UserModel(
    var uid: String = "",
    var displayName: String? = null,
    var email: String? = null,
    var phoneNumber: String? = null,
    var profilePictureUrl: String? = null, // Sẽ lưu định danh ảnh, ví dụ "avatar_1"
    var bio: String? = null,
    var inviteCode: String? = null, // Mã mời của người dùng
    var friendIds: List<String> = emptyList(), // Danh sách ID của bạn bè
    var sentFriendRequests: List<String> = emptyList(), // ID của người mình đã gửi lời mời
    var receivedFriendRequests: List<String> = emptyList(), // ID của người đã gửi lời mời cho mình
    // Các trường vị trí
    var latitude: Double? = null,
    var longitude: Double? = null,

    @ServerTimestamp
    var createdAt: Timestamp? = null,

    @ServerTimestamp
    var lastUpdated: Timestamp? = null
)
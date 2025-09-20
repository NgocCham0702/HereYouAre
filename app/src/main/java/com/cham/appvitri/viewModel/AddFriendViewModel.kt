package com.cham.appvitri.viewModel

import android.util.Log
import com.google.firebase.firestore.QuerySnapshot // Đảm bảo bạn có import này

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.FriendRequestModel
import com.cham.appvitri.model.FriendRequestStatus
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.repository.ChatRepository
import com.cham.appvitri.repository.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

// Lớp này kết hợp thông tin lời mời và thông tin người gửi để UI dễ dàng hiển thị
data class FriendRequestWithSender(
    val request: FriendRequestModel,
    val sender: UserModel
)
// Đặt bên ngoài class AddFriendViewModel
enum class FriendshipStatus {
    NOT_FRIENDS,
    REQUEST_SENT,
    IS_FRIEND,
    SELF
}

data class UserWithStatus(
    val user: UserModel,
    val status: FriendshipStatus
)
class AddFriendViewModel : ViewModel() {

    // --- Khởi tạo Firebase ---
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")
    private val requestsCollection = db.collection("friend_requests")
    // --- Lấy thông tin người dùng hiện tại ---
    private val currentUserId: String? get() = auth.currentUser?.uid
    // --- Các Biến Trạng Thái (State) cho UI ---
    // Trạng thái chung để hiển thị vòng xoay tải dữ liệu
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    // Mã mời cá nhân của người dùng
    private val _personalCode = MutableStateFlow("Đang tải...")
    val personalCode: StateFlow<String> = _personalCode
    // Danh sách những người đã là bạn
    private val _friends = MutableStateFlow<List<UserModel>>(emptyList())
    val friends: StateFlow<List<UserModel>> = _friends
    // Danh sách lời mời kết bạn đã nhận
    private val _receivedRequests = MutableStateFlow<List<FriendRequestWithSender>>(emptyList())
    val receivedRequests: StateFlow<List<FriendRequestWithSender>> = _receivedRequests
    // Nội dung ô tìm kiếm
    var searchQuery = mutableStateOf("")
        private set
    // Kết quả tìm kiếm người dùng
    private val _searchResult = MutableStateFlow<List<UserWithStatus>>(emptyList())
    val searchResult: StateFlow<List<UserWithStatus>> = _searchResult
    // Tin nhắn tạm thời cho UI (ví dụ: "Đã gửi lời mời!")
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage
    //private val chatRepository = ChatRepository() // Giả sử bạn đã có
    private val userRepository = UserRepository() // Giả sử bạn đã có
    private val chatRepository = ChatRepository()

    init {
        // Bắt đầu tải dữ liệu ngay khi ViewModel được tạo
        currentUserId?.let { uid ->
            // Sử dụng listener để dữ liệu tự động cập nhật
            listenToUserData(uid)
            listenToFriends(uid)
            listenToFriendRequests(uid)
            listenToSentFriendRequests(uid)
        } ?: run {
            _uiMessage.value = "Lỗi: Người dùng chưa đăng nhập."
        }
    }

    // --- Các Hàm Lắng nghe Dữ liệu (Real-time) ---

    private fun listenToUserData(uid: String) {
        usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                _uiMessage.value = "Lỗi tải dữ liệu người dùng."
                return@addSnapshotListener
            }
            snapshot?.toObject<UserModel>()?.let { user ->
                _personalCode.value = user.personalCode ?: "Chưa có mã"
            }
        }
    }

    private fun listenToFriends(uid: String) {
        usersCollection.document(uid).addSnapshotListener { snapshot, _ ->
            val friendUids = snapshot?.toObject<UserModel>()?.friendUids ?: emptyList()
            if (friendUids.isNotEmpty()) {
                // Lấy thông tin chi tiết của từng người bạn
                usersCollection.whereIn("uid", friendUids).get().addOnSuccessListener { friendSnapshots ->
                    _friends.value = friendSnapshots.toObjects(UserModel::class.java)
                }
            } else {
                _friends.value = emptyList()
            }
        }
    }

    private fun listenToFriendRequests(uid: String) {
        requestsCollection
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", FriendRequestStatus.PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiMessage.value = "Lỗi tải danh sách lời mời."
                    return@addSnapshotListener
                }
                viewModelScope.launch {
                    val requestsWithSenders = snapshot?.documents?.mapNotNull { doc ->
                        // --- ĐÂY LÀ PHẦN SỬA LỖI ---
                        // 1. Chuyển đổi document thành model
                        val request = doc.toObject<FriendRequestModel>()
                        if (request != null) {
                            // 2. Lấy ID của document và gán vào model
                            request.requestId = doc.id

                            // 3. Lấy thông tin của người gửi (logic này không đổi)
                            usersCollection.document(request.fromUid).get().await().toObject<UserModel>()?.let { sender ->
                                FriendRequestWithSender(request, sender)
                            }
                        } else {
                            null
                        }
                        // --- KẾT THÚC SỬA LỖI ---
                    } ?: emptyList()

                    _receivedRequests.value = requestsWithSenders
                }
            }
    }
    // --- Các Hàm Xử Lý Sự Kiện từ UI ---

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun searchUsers() {
        val query = searchQuery.value.trim()
        if (query.isBlank()) {
            _searchResult.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Tìm kiếm user
                val byCodeDeferred = async { usersCollection.whereEqualTo("personalCode", query).get().await() }
                val byEmailDeferred = async { usersCollection.whereEqualTo("email", query).get().await() }
                val byPhoneDeferred = async { usersCollection.whereEqualTo("phoneNumber", query).get().await() }
                val results: List<QuerySnapshot> = awaitAll(byCodeDeferred, byEmailDeferred, byPhoneDeferred)

                val uniqueUsers = mutableMapOf<String, UserModel>()
                results.forEach { snapshot ->
                    snapshot.toObjects(UserModel::class.java).forEach { user ->
                        // Không thêm chính mình vào kết quả
                        if (user.uid != currentUserId) {
                            uniqueUsers[user.uid] = user
                        }
                    }
                }

                // Lấy danh sách ID của bạn bè và người đã gửi lời mời để so sánh
                val friendsUids = _friends.value.map { it.uid }.toSet()
                val sentRequestUids = _sentRequests.value.map { it.receiver.uid }.toSet()

                // Chuyển đổi List<UserModel> thành List<UserWithStatus>
                val resultWithStatus = uniqueUsers.values.map { user ->
                    val status = when {
                        // user.uid == currentUserId đã được lọc ở trên, nên không cần nữa
                        friendsUids.contains(user.uid) -> FriendshipStatus.IS_FRIEND
                        sentRequestUids.contains(user.uid) -> FriendshipStatus.REQUEST_SENT
                        else -> FriendshipStatus.NOT_FRIENDS
                    }
                    UserWithStatus(user, status)
                }

                _searchResult.value = resultWithStatus

                if (resultWithStatus.isEmpty()) {
                    _uiMessage.value = "Không tìm thấy người dùng nào."
                }

            } catch (e: Exception) {
                _uiMessage.value = "Tìm kiếm thất bại: ${e.message}"
                Log.e("AddFriendViewModel", "Search failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendFriendRequest(toUser: UserModel) {
        viewModelScope.launch {
            val fromUid = currentUserId ?: return@launch
            val toUid = toUser.uid
            // Kiểm tra xem đã gửi lời mời hoặc đã là bạn bè chưa
            // (Thêm logic kiểm tra này nếu cần để tối ưu)
            val currentUserDoc = usersCollection.document(fromUid).get().await()
            val fromUserName = currentUserDoc.getString("displayName")
            val newRequest = FriendRequestModel(
                fromUid = fromUid,
                toUid = toUid,
                status = FriendRequestStatus.PENDING,
                fromName = fromUserName,      // <<< THÊM fromName
                toName = toUser.displayName
            )

            requestsCollection.add(newRequest).await()
            _uiMessage.value = "Đã gửi lời mời đến ${toUser.displayName}."
        }
    }

    /*fun acceptFriendRequest(requestWithSender: FriendRequestWithSender) {
        viewModelScope.launch {
            // --- SỬA LỖI: Kiểm tra requestId trước khi sử dụng ---
            val requestDocId = requestWithSender.request.requestId
            if (requestDocId.isBlank()) {
                _uiMessage.value = "Lỗi: Không tìm thấy ID của lời mời."
                return@launch
            }
            // --- KẾT THÚC SỬA LỖI ---

            val fromUser = requestWithSender.sender
            val toUid = currentUserId ?: return@launch

            try {
                db.runBatch { batch ->
                    val requestRef = requestsCollection.document(requestDocId)
                    batch.update(requestRef, "status", FriendRequestStatus.ACCEPTED)

                    val currentUserRef = usersCollection.document(toUid)
                    batch.update(currentUserRef, "friendUids", FieldValue.arrayUnion(fromUser.uid))

                    val fromUserRef = usersCollection.document(fromUser.uid)
                    batch.update(fromUserRef, "friendUids", FieldValue.arrayUnion(toUid))
                }.await()
                _uiMessage.value = "Đã kết bạn với ${fromUser.displayName}."
            } catch (e: Exception) {
                _uiMessage.value = "Có lỗi xảy ra: ${e.message}"
            }
        }
    }*/
    fun acceptFriendRequest(requestWithSender: FriendRequestWithSender) {
        viewModelScope.launch(Dispatchers.IO) { // Chuyển sang IO Context vì có nhiều thao tác DB
            val requestDocId = requestWithSender.request.requestId
            if (requestDocId.isBlank()) {
                withContext(Dispatchers.Main) {
                    _uiMessage.value = "Lỗi: Không tìm thấy ID của lời mời."
                }
                return@launch
            }

            val fromUser = requestWithSender.sender
            val toUid = currentUserId ?: return@launch

            try {
                // Bước 1: Chấp nhận lời mời và thêm bạn bè
                db.runBatch { batch ->
                    val requestRef = requestsCollection.document(requestDocId)
                    batch.update(requestRef, "status", FriendRequestStatus.ACCEPTED)

                    val currentUserRef = usersCollection.document(toUid)
                    batch.update(currentUserRef, "friendUids", FieldValue.arrayUnion(fromUser.uid))

                    val fromUserRef = usersCollection.document(fromUser.uid)
                    batch.update(fromUserRef, "friendUids", FieldValue.arrayUnion(toUid))
                }.await()

                // Cập nhật UI trên Main thread
                withContext(Dispatchers.Main) {
                    _uiMessage.value = "Đã kết bạn với ${fromUser.displayName}."
                }

                // Bước 2: TỰ ĐỘNG TẠO CUỘC TRÒ CHUYỆN
                Log.d("AcceptFriend", "Kết bạn thành công! Bắt đầu tạo phòng chat giữa $toUid và ${fromUser.uid}")
                try {
                    val chatId = chatRepository.createChatForTwoUsers(userId1 = toUid, userId2 = fromUser.uid)
                    Log.d("AcceptFriend", "Tạo/tìm thấy phòng chat thành công. ID: $chatId")
                } catch (chatError: Exception) {
                    Log.e("AcceptFriend", "Lỗi nghiêm trọng khi tạo phòng chat: ", chatError)
                    // Bạn có thể thêm một thông báo lỗi khác ở đây nếu cần
                }

            } catch (e: Exception) {
                Log.e("AcceptFriend", "Lỗi khi chấp nhận lời mời: ", e)
                withContext(Dispatchers.Main) {
                    _uiMessage.value = "Có lỗi xảy ra: ${e.message}"
                }
            }
        }
    }
    fun declineFriendRequest(requestWithSender: FriendRequestWithSender) {
        viewModelScope.launch {
            // --- SỬA LỖI: Kiểm tra requestId trước khi sử dụng ---
            val requestDocId = requestWithSender.request.requestId
            if (requestDocId.isBlank()) {
                _uiMessage.value = "Lỗi: Không tìm thấy ID của lời mời."
                return@launch
            }
            // --- KẾT THÚC SỬA LỖI ---

            try {
                requestsCollection.document(requestDocId)
                    .update("status", FriendRequestStatus.DECLINED).await()
                _uiMessage.value = "Đã từ chối lời mời."
            } catch (e: Exception) {
                _uiMessage.value = "Có lỗi xảy ra: ${e.message}"
            }
        }
    }

    fun deleteFriend(friend: UserModel) {
        viewModelScope.launch {
            val friendUid = friend.uid
            val currentUserUid = currentUserId ?: return@launch

            db.runBatch { batch ->
                // 1. Xóa bạn khỏi danh sách của người dùng hiện tại
                val currentUserRef = usersCollection.document(currentUserUid)
                batch.update(currentUserRef, "friendUids", FieldValue.arrayRemove(friendUid))

                // 2. Xóa người dùng hiện tại khỏi danh sách của người bạn kia
                val friendRef = usersCollection.document(friendUid)
                batch.update(friendRef, "friendUids", FieldValue.arrayRemove(currentUserUid))
            }.await()
            _uiMessage.value = "Đã xóa ${friend.displayName} khỏi danh sách bạn bè."
        }
    }

    fun onMessageShown() {
        _uiMessage.value = null
    }
    // BIẾN MỚI: Danh sách lời mời đã gửi
    private val _sentRequests = MutableStateFlow<List<FriendRequestWithReceiver>>(emptyList())
    val sentRequests: StateFlow<List<FriendRequestWithReceiver>> = _sentRequests
    // Data class mới để gộp thông tin lời mời và người nhận
    data class FriendRequestWithReceiver(
        val request: FriendRequestModel,
        val receiver: UserModel
    )

    // HÀM MỚI: Lắng nghe các lời mời đã gửi
    // HÀM MỚI: Lắng nghe các lời mời đã gửi
    private fun listenToSentFriendRequests(uid: String) {
        requestsCollection
            .whereEqualTo("fromUid", uid)
            .whereEqualTo("status", FriendRequestStatus.PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Xử lý lỗi
                    return@addSnapshotListener
                }
                viewModelScope.launch {
                    val requestsWithReceivers = snapshot?.documents?.mapNotNull { doc ->
                        val request = doc.toObject<FriendRequestModel>()
                        if (request != null) {
                            request.requestId = doc.id   // ✅ GÁN ID document vào request
                            usersCollection.document(request.toUid).get().await()
                                .toObject<UserModel>()?.let { receiver ->
                                    FriendRequestWithReceiver(request, receiver)
                                }
                        } else null
                    } ?: emptyList()

                    _sentRequests.value = requestsWithReceivers
                }
            }
    }


    // THÊM HÀM MỚI: Để hủy lời mời đã gửi
    fun cancelFriendRequest(requestWithReceiver: FriendRequestWithReceiver) {
        viewModelScope.launch {
            // Cần đảm bảo requestId được lưu trong document khi tạo
            val requestDocId = requestWithReceiver.request.requestId
            Log.d("CancelRequest", "authUid=${Firebase.auth.currentUser?.uid}, fromUid=${requestWithReceiver.request.fromUid}")

            try {
                requestsCollection.document(requestDocId).delete().await()
                _uiMessage.value = "Đã hủy lời mời."
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi khi hủy lời mời: ${e.message}"
            }
        }
    }

}
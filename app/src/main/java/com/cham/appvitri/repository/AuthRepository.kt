package com.cham.appvitri.repository

import com.cham.appvitri.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth // <-- This line is crucial
import com.google.firebase.auth.FirebaseUser // You'll also need this for getCurrentUser
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    /**
     * Lấy người dùng đang đăng nhập hiện tại.
     * @return FirebaseUser? - Trả về đối tượng người dùng nếu đã đăng nhập, ngược lại trả về null.
     */
    fun getCurrentUser(): FirebaseUser? {return auth.currentUser}
    //TODO : cac ham dang ky dang nhap ...
    /**
     * Lấy UID của người dùng đang đăng nhập hiện tại.
     * Cách dùng an toàn và ngắn gọn.
     * @return String? - Trả về UID nếu đã đăng nhập, ngược lại trả về null.
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    /**
     * Hàm đăng nhập người dùng bằng email và mật khẩu.
     * @param email Email của người dùng.
     * @param password Mật khẩu của người dùng.
     * @return Result<FirebaseUser> - Trả về thành công với FirebaseUser hoặc thất bại với Exception.
     */
    suspend fun loginUserWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult: AuthResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after login.")
            Result.success(user)
        } catch (e: Exception) {
            // Các lỗi thường gặp: sai mật khẩu, email không tồn tại...
            Result.failure(e)
        }
    }
    suspend fun registerUserWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult: AuthResult = auth.createUserWithEmailAndPassword(email, password).await()
            // Nếu không có user, ném ra một exception để khối catch bắt được
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after registration.")
            Result.success(user)
        } catch (e: Exception) {
            // Các lỗi thường gặp: email đã tồn tại, mật khẩu yếu, không có mạng...
            Result.failure(e)
        }
    }
    fun logout(){
        auth.signOut()
    }
    /**
     * Hàm đăng ký người dùng mới bằng email và mật khẩu.
     * Được bọc trong Result để xử lý lỗi một cách an toàn.
     * @param email Email của người dùng.
     * @param password Mật khẩu của người dùng.
     * @return Result<FirebaseUser> - Trả về thành công với FirebaseUser hoặc thất bại với Exception.
     */

}
class UserRepository {
    private val usersCollection = Firebase.firestore.collection("users")
    // Hàm lưu toàn bộ thông tin người dùng vào Firestore
    // Dùng uid làm key cho document
    suspend fun saveUserProfile(user: UserModel): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    /**
     * <<< HÀM MỚI BẠN CẦN >>>
     * Lắng nghe sự thay đổi của một hồ sơ người dùng trong thời gian thực.
     * @param userId ID của người dùng cần lắng nghe.
     * @return Một Flow phát ra đối tượng UserModel mỗi khi có sự thay đổi.
     */
    fun getUserProfileFlow(userId: String): Flow<UserModel?> = callbackFlow {
        // Tham chiếu đến document của người dùng cụ thể
        val docRef = usersCollection.document(userId)

        // Đăng ký một listener để lắng nghe sự thay đổi
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            // Nếu có lỗi từ Firestore (ví dụ: không có quyền truy cập)
            if (error != null) {
                // Đóng Flow và báo lỗi
                close(error)
                return@addSnapshotListener
            }

            // Nếu snapshot tồn tại, chuyển đổi nó thành UserModel
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(UserModel::class.java)
                // Gửi (trySend) đối tượng user vào Flow
                trySend(user).isSuccess
            } else {
                // Trường hợp document không tồn tại (ví dụ: người dùng đã bị xóa)
                trySend(null).isSuccess
            }
        }

        // Khối này sẽ được thực thi khi Flow bị hủy (coroutine scope bị hủy)
        // Rất quan trọng để gỡ bỏ listener, tránh rò rỉ bộ nhớ!
        awaitClose {
            subscription.remove()
        }
    }

    /**
     * Hàm cập nhật chỉ vị trí của người dùng mà không ghi đè các thông tin khác.
     * @param userId ID của người dùng.
     * @param latitude Vĩ độ mới.
     * @param longitude Kinh độ mới.
     * @return Result cho biết thành công hay thất bại.
     */
    suspend fun updateUserLocation(userId: String, latitude: Double, longitude: Double): Result<Unit> {
        return try {
            val locationData = mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "lastUpdated" to FieldValue.serverTimestamp()
            )
            usersCollection.document(userId).update(locationData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
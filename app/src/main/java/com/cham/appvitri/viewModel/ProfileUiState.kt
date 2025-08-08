// file: com/cham/appvitri/viewModel/ProfileViewModel.kt
package com.cham.appvitri.viewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.utils.ImageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: UserModel? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val editedDisplayName: String = "",
    val editedBio: String = "",
    val editedPhoneNumber: String = "",
    val selectedAvatarUri: Uri? = null
)

class ProfileViewModel(
    private val application: Application,
    private val userId: String,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

// Đặt hàm này vào bên trong class ProfileViewModel

    private fun loadUserProfile() {
        viewModelScope.launch {
            // Bắt đầu lắng nghe dòng dữ liệu từ Firestore thông qua repository
            userRepository.getUserProfileFlow(userId)
                .catch { e ->
                    // Nếu có lỗi trong quá trình lắng nghe (ví dụ: không có quyền, lỗi mạng...)
                    // Cập nhật state để báo lỗi cho UI
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, // Đã tải xong (dù thất bại)
                        errorMessage = "Lỗi tải thông tin: ${e.message}"
                    )
                }
                .collect { userModel ->
                    // Mỗi khi có dữ liệu mới từ Firestore (lần đầu hoặc khi có cập nhật)
                    // userModel sẽ chứa dữ liệu mới nhất (hoặc null nếu document bị xóa)

                    if (userModel != null) {
                        // Cập nhật toàn bộ UI state với dữ liệu nhận được
                        _uiState.value = _uiState.value.copy(
                            isLoading = false, // Đã tải xong
                            user = userModel, // Dữ liệu gốc từ Firestore
                            // Đồng thời, điền dữ liệu này vào các trường chỉnh sửa
                            // để người dùng thấy giá trị hiện tại trước khi sửa.
                            editedDisplayName = userModel.displayName ?: "",
                            editedBio = userModel.bio ?: "",
                            editedPhoneNumber = userModel.phoneNumber ?: ""
                            // Không cần cập nhật selectedAvatarUri ở đây,
                            // vì nó chỉ thay đổi khi người dùng chọn ảnh mới.
                        )
                    } else {
                        // Xử lý trường hợp không tìm thấy người dùng (ví dụ: tài khoản đã bị xóa)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Không tìm thấy thông tin người dùng."
                        )
                    }
                }
        }
    }
    fun onDisplayNameChange(newName: String) { _uiState.value = _uiState.value.copy(editedDisplayName = newName) }
    fun onBioChange(newBio: String) { _uiState.value = _uiState.value.copy(editedBio = newBio) }
    fun onPhoneNumberChange(newPhone: String) { _uiState.value = _uiState.value.copy(editedPhoneNumber = newPhone) }
    fun onAvatarSelected(uri: Uri) { _uiState.value = _uiState.value.copy(selectedAvatarUri = uri) }

    fun saveChanges() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            var newAvatarData = _uiState.value.user?.profilePictureUrl

            _uiState.value.selectedAvatarUri?.let { uri ->
                val base64String = ImageHelper.uriToBase64(application, uri)
                if (base64String != null) {
                    newAvatarData = base64String
                } else {
                    _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "Lỗi xử lý ảnh.")
                    return@launch
                }
            }

            val updatedUser = _uiState.value.user?.copy(
                displayName = _uiState.value.editedDisplayName,
                bio = _uiState.value.editedBio,
                phoneNumber = _uiState.value.editedPhoneNumber,
                profilePictureUrl = newAvatarData ?: ""
            ) ?: return@launch

            // --- SỬA LỖI TẠI ĐÂY ---
            val saveResult = userRepository.saveUserProfile(updatedUser)

            if (saveResult.isSuccess) {
                // Thành công
                _uiState.value = _uiState.value.copy(isSaving = false)
            } else {
                // Thất bại
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Lỗi lưu thông tin: ${saveResult.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    fun logout() { authRepository.logout() }
}
// file: com/cham/appvitri/viewModel/ProfileViewModel.kt
package com.cham.appvitri.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: UserModel? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    // Thêm biến để quản lý chế độ sửa
    val isInEditMode: Boolean = false,

    // Dữ liệu người dùng nhập vào
    val editedDisplayName: String = "",
    val editedBio: String = "",
    val editedPhoneNumber: String = "",
    val selectedAvatarIdentifier: String? = null
)

class ProfileViewModel(
    private val userId: String,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            userRepository.getUserProfileFlow(userId).collect { userModel ->
                _uiState.update { currentState ->
                    // --- LOGIC SỬA LỖI NẰM Ở ĐÂY ---
                    // Chỉ cập nhật các trường `edited...` nếu người dùng
                    // KHÔNG ở trong chế độ sửa.
                    if (currentState.isInEditMode) {
                        // Nếu đang sửa, chỉ cập nhật dữ liệu gốc `user`
                        // và giữ nguyên những gì người dùng đang gõ.
                        currentState.copy(

                            isLoading = false,
                            user = userModel
                        )
                    } else {
                        // Nếu không ở chế độ sửa, đồng bộ cả dữ liệu gốc và dữ liệu edit.
                        currentState.copy(
                            isLoading = false,
                            user = userModel,
                            editedDisplayName = userModel?.displayName ?: "",
                            editedBio = userModel?.bio ?: "",
                            editedPhoneNumber = userModel?.phoneNumber ?: "",
                            selectedAvatarIdentifier = userModel?.profilePictureUrl
                        )
                    }
                }
            }
        }
    }

    // --- Các hàm xử lý sự kiện từ UI ---
    fun onDisplayNameChange(newName: String) { _uiState.update { it.copy(editedDisplayName = newName) } }
    fun onBioChange(newBio: String) { _uiState.update { it.copy(editedBio = newBio) } }
    fun onPhoneNumberChange(newPhone: String) { _uiState.update { it.copy(editedPhoneNumber = newPhone) } }
    fun onAvatarSelected(identifier: String) { _uiState.update { it.copy(selectedAvatarIdentifier = identifier) } }

    // --- Các hàm quản lý chế độ sửa ---
    fun onEditModeToggled() {
        val currentState = _uiState.value
        // Khi bật chế độ sửa, đảm bảo các trường edit được reset về giá trị gốc
        _uiState.value = currentState.copy(
            isInEditMode = !currentState.isInEditMode,
            editedDisplayName = currentState.user?.displayName ?: "",
            editedBio = currentState.user?.bio ?: "",
            editedPhoneNumber = currentState.user?.phoneNumber ?: "",
            selectedAvatarIdentifier = currentState.user?.profilePictureUrl
        )
    }

    // Hủy bỏ chỉnh sửa (chỉ cần tắt chế độ sửa, dữ liệu edit sẽ tự reset ở lần bật sau)
    fun onCancelEdit() { _uiState.update { it.copy(isInEditMode = false) } }

    fun saveChanges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val updatedUser = _uiState.value.user?.copy(
                displayName = _uiState.value.editedDisplayName.trim(),
                bio = _uiState.value.editedBio.trim(),
                phoneNumber = _uiState.value.editedPhoneNumber.trim(),
                profilePictureUrl = _uiState.value.selectedAvatarIdentifier ?: "default"
            ) ?: return@launch

            val saveResult = userRepository.saveUserProfile(updatedUser)

            // Dù thành công hay thất bại, tắt chế độ sửa và trạng thái saving
            if (saveResult.isSuccess) {
                _uiState.update { it.copy(isSaving = false, isInEditMode = false) }
                // TODO: Gửi sự kiện "Lưu thành công" lên UI để hiển thị Toast
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Lỗi khi lưu thông tin.") }
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
// 3. FACTORY KHÔNG CẦN THAY ĐỔI
class ProfileVMFactory(
    private val userId: String,
    private val userRepo: UserRepository,
    private val authRepo: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userId, userRepo, authRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
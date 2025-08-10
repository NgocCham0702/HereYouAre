// file: com/cham/appvitri/view/ProfileScreen.kt
package com.cham.appvitri.view

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.utils.AvatarHelper
import com.cham.appvitri.viewModel.ProfileUiState
import com.cham.appvitri.viewModel.ProfileVMFactory
import com.cham.appvitri.viewModel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType


val primaryBlue = Color(0xFF4A85F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onBackClicked: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val context = LocalContext.current
    val factory = remember(userId) { ProfileVMFactory(userId, UserRepository(), AuthRepository()) }
    val viewModel: ProfileViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    // Lắng nghe trạng thái đăng nhập
    val isLoggedIn by remember { derivedStateOf { FirebaseAuth.getInstance().currentUser != null } }
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) onLogoutSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isInEditMode) "Chỉnh sửa hồ sơ" else "Hồ sơ của tôi") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    // Nút Sửa/Hủy chỉ hiển thị khi đã tải xong và không đang lưu
                    // Nút Hủy sẽ là nút Back ở TopAppBar khi đang ở chế độ sửa
                    if (uiState.isInEditMode) {
                        TextButton(onClick = viewModel::onCancelEdit) { Text("HỦY") }
                    } else {
                        IconButton(onClick = viewModel::onEditModeToggled) { Icon(Icons.Default.Edit, "Sửa") }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.user == null -> Text("Không tìm thấy người dùng.", modifier = Modifier.align(Alignment.Center))
                else -> {
                    // Hiển thị giao diện
                    ProfileContent(
                        uiState = uiState,
                        onDisplayNameChange = viewModel::onDisplayNameChange,
                        onBioChange = viewModel::onBioChange,
                        onPhoneNumberChange = viewModel::onPhoneNumberChange,
                        onAvatarSelected = viewModel::onAvatarSelected,
                        onSaveClick = {
                            viewModel.saveChanges()
                            Toast.makeText(context, "Đã lưu thay đổi!", Toast.LENGTH_SHORT).show()
                        },
                        onLogoutClick = viewModel::logout
                    )
                }
            }

            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onSaveClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- AVATAR ---
        Box {
            Image(
                painter = painterResource(id = AvatarHelper.getDrawableId(if (uiState.isInEditMode) uiState.selectedAvatarIdentifier else uiState.user?.profilePictureUrl)),
                contentDescription = "Avatar",
                modifier = Modifier.size(120.dp).clip(CircleShape)
            )
            if (uiState.isInEditMode) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change Avatar",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(primaryBlue, CircleShape)
                        .padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- DANH SÁCH AVATAR (chỉ hiển thị ở chế độ sửa) ---
        AnimatedVisibility(visible = uiState.isInEditMode) {
            Column {
                Text("Chọn ảnh đại diện mới", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(AvatarHelper.avatars.entries.toList()) { (identifier, drawableId) ->
                        val isSelected = uiState.selectedAvatarIdentifier == identifier
                        Image(
                            painter = painterResource(id = drawableId),
                            contentDescription = identifier,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isSelected) primaryBlue else Color.LightGray, CircleShape)
                                .clickable { onAvatarSelected(identifier) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- CÁC TRƯỜNG THÔNG TIN ---
        InfoRow(label = "Tên", isInEditMode = uiState.isInEditMode, value = uiState.editedDisplayName, onValueChange = onDisplayNameChange)
        InfoRow(label = "Email", value = uiState.user?.email ?: "...")
        InfoRow(label = "SĐT",isInEditMode = uiState.isInEditMode, value = uiState.editedPhoneNumber, onValueChange = onPhoneNumberChange)
        InfoRow(label = "Ghi chú", isInEditMode = uiState.isInEditMode, value = uiState.editedBio, onValueChange = onBioChange)

        Spacer(modifier = Modifier.weight(1f))

        // --- NÚT LƯU VÀ ĐĂNG XUẤT (chỉ hiển thị ở chế độ sửa) ---
        AnimatedVisibility(visible = uiState.isInEditMode) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveClick, modifier = Modifier.fillMaxWidth()) {
                    Text("LƯU THAY ĐỔI")
                }
            }
        }
        // Nút ĐĂNG XUẤT chỉ hiện khi KHÔNG sửa
        AnimatedVisibility(visible = !uiState.isInEditMode) {
            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("ĐĂNG XUẤT")
            }
        }
    }
}

// Composable InfoRow được nâng cấp để hỗ trợ cả 2 chế độ
@Composable
fun InfoRow(
    label: String,
    value: String,
    isInEditMode: Boolean = false,
    onValueChange: (String) -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(90.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        // Nếu ở chế độ sửa thì hiện TextField, không thì hiện Text
        if (isInEditMode) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
            )
        } else {
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
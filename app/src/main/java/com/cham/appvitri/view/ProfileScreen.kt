// file: com/cham/appvitri/view/ProfileScreen.kt
package com.cham.appvitri.view

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cham.appvitri.R
import com.cham.appvitri.repository.AuthRepository
import com.cham.appvitri.repository.UserRepository
import com.cham.appvitri.viewModel.*

val primaryBlue = Color(0xFF4A85F6)

@Composable
fun ProfileScreen(
    navController: NavController,
    userId: String?
) {
    if (userId.isNullOrBlank()) {
        // Xử lý lỗi
        return
    }

    val context = LocalContext.current
    val factory = remember(userId) {
        ProfileViewModelFactory(
            context.applicationContext as Application,
            userId,
            UserRepository(),
            AuthRepository()
        )
    }
    val viewModel: ProfileViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onAvatarSelected(it) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.errorMessage != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Lỗi: ${uiState.errorMessage}") }
            uiState.user != null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()) // Cho phép cuộn nếu nội dung dài
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UserProfileCard(
                            uiState = uiState,
                            onBackClick = { navController.popBackStack() },
                            onLogoutClick = { viewModel.logout() },
                            onDisplayNameChange = viewModel::onDisplayNameChange,
                            onBioChange = viewModel::onBioChange,
                            onPhoneNumberChange = viewModel::onPhoneNumberChange,
                            onCancelClick = { navController.popBackStack() },
                            onSaveClick = viewModel::saveChanges,
                            onAvatarChangeClick = { imagePickerLauncher.launch("image/*") }
                        )
                    }

                    if (uiState.isSaving) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileCard(
    uiState: ProfileUiState,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit,
    onAvatarChangeClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProfileHeader(uiState, onBackClick, onAvatarChangeClick, onDisplayNameChange)
            ProfileBody(uiState, onLogoutClick, onDisplayNameChange, onBioChange, onPhoneNumberChange, onCancelClick, onSaveClick)
        }
    }
}

@Composable
fun ProfileHeader(uiState: ProfileUiState, onBackClick: () -> Unit, onAvatarChangeClick: () -> Unit,onDisplayNameChange: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(primaryBlue)) {
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val imageData = remember(uiState.selectedAvatarUri, uiState.user?.profilePictureUrl) {
                when {
                    uiState.selectedAvatarUri != null -> uiState.selectedAvatarUri
                    !uiState.user?.profilePictureUrl.isNullOrBlank() -> {
                        val url = uiState.user!!.profilePictureUrl
                        if (url.startsWith("http")) url else try { Base64.decode(url, Base64.DEFAULT) } catch (e: Exception) { R.drawable.no}
                    }
                    else -> R.drawable.no
                }
            }
            AsyncImage(
                model = imageData,
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape)
                    .clickable { onAvatarChangeClick() }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.editedDisplayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Tên hiển thị", color = Color.White) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.White, unfocusedIndicatorColor = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
fun ProfileBody(
    uiState: ProfileUiState,
    onLogoutClick: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        OutlinedTextField(value = uiState.user?.email ?: "...", onValueChange = {}, label = { Text("Email (không thể đổi)") }, enabled = false, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = uiState.editedPhoneNumber, onValueChange = onPhoneNumberChange, label = { Text("Số điện thoại") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = uiState.editedBio, onValueChange = onBioChange, label = { Text("Ghi chú / Bio") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onSaveClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) { Text("Lưu") }
            Button(onClick = onCancelClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) { Text("Hủy") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.Red)) {
            Text("Đăng xuất", color = Color.Red)
        }
    }
}
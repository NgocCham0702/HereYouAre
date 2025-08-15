package com.cham.appvitri.view // Hoặc package View của bạn

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cham.appvitri.model.UserModel
import com.cham.appvitri.utils.AvatarHelper
import com.cham.appvitri.viewModel.CreateGroupViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
    viewModel: CreateGroupViewModel = viewModel()
) {
    val friends by viewModel.friends.collectAsState()
    val selectedFriendIds by viewModel.selectedFriendIds.collectAsState()
    var groupName by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Tạo nhóm mới") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        floatingActionButton = {
            val canCreate = selectedFriendIds.isNotEmpty() && groupName.isNotBlank()
            FloatingActionButton(
                onClick = {
                    if (canCreate) {
                        viewModel.createGroup(groupName, onGroupCreated)
                    }
                },
                containerColor = if (canCreate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Default.Done, contentDescription = "Tạo nhóm")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Tên nhóm") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Chọn thành viên:", style = MaterialTheme.typography.titleMedium)
            
            if (friends.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bạn chưa có bạn bè nào để tạo nhóm.")
                }
            } else {
                LazyColumn {
                    items(friends) { friend ->
                        FriendSelectItem(
                            friend = friend,
                            isSelected = selectedFriendIds.contains(friend.uid),
                            onSelectionChange = {
                                viewModel.toggleFriendSelection(friend.uid)
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
 fun FriendSelectItem(
    friend: UserModel,
    isSelected: Boolean,
    onSelectionChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectionChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = AvatarHelper.getDrawableId(friend.profilePictureUrl)),
            contentDescription = friend.displayName,
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )
        Text(
            text = friend.displayName ?: "Người dùng",
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        Checkbox(checked = isSelected, onCheckedChange = { onSelectionChange() })
    }
}
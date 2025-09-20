package com.cham.appvitri.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cham.appvitri.viewModel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    navController: NavController,
    viewModel: AdminViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val requests by viewModel.requests.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Users", "Friend Requests")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("adminPanel") { inclusive = true } // xóa stack
                        }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Đăng xuất")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nội dung tab
            when (selectedTab) {
                0 -> {
                    // Users list
                    LazyColumn {
                        items(users) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(user.displayName ?: user.uid ?: "No name")
                                Button(
                                    onClick = { viewModel.deleteUser(user.uid ?: return@Button) },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Xóa")
                                }
                            }
                            Divider()
                        }
                    }
                }
                1 -> {
                    // Friend Requests list
                    LazyColumn {
                        items(requests) { req ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Cho phép Text dài xuống dòng
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${req.fromName ?: "?"} → ${req.toName ?: "?"}")
                                        Text(
                                            text = "Trạng thái: ${req.status ?: "?"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.deleteRequest(req.requestId ?: return@Button) },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Xóa")
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }

            }
        }
    }
}
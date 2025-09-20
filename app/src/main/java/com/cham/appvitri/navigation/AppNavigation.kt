// file: com/cham/appvitri/navigation/AppNavigation.kt
package com.cham.appvitri.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cham.appvitri.view.*
import com.cham.appvitri.view.account.LoginScreen
import com.cham.appvitri.view.login.RegisterScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // startDestination LUÔN LUÔN là "splash"
    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen() // Hiển thị vòng xoay loading

            LaunchedEffect(Unit) {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    try {
                        val doc = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUser.uid)
                            .get()
                            .await()

                        val role = doc.getString("role") ?: "user"

                        if (role == "admin") {
                            navController.navigate("adminPanel") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else {
                            navController.navigate("home/${currentUser.uid}") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    } catch (e: Exception) {
                        // Nếu lỗi thì coi như user thường
                        navController.navigate("home/${currentUser.uid}") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                } else {
                    navController.navigate("welcome") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                onPasswordResetSuccess = {
                    // Quay lại màn hình login sau khi thành công
                    navController.popBackStack()
                }
            )
        }
        composable("welcome") { WelcomeScreen(navController = navController) }

        composable("login") {
            LoginScreen(
                navController = navController,
                onForgotPasswordClicked = {
                    navController.navigate("forgot_password")
                },
                onLoginSuccess = { userId ->
                    navController.navigate("home/$userId") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        composable("register") { RegisterScreen(navController = navController) }

        composable(
            route = "home/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")

            // Chỉ hiển thị HomeScreen KHI VÀ CHỈ KHI userId hợp lệ
            if (!userId.isNullOrBlank()) {
                HomeScreen(
                    userId = userId,
                    onAvatarClicked = {
                        // userId ở đây được đảm bảo là không rỗng
                        navController.navigate("profile/$userId")
                    },
                    onSosClicked = { navController.navigate("sos") },
                    onAddFriendClicked = { navController.navigate("add_friend") },
                    onEventsClicked = { navController.navigate("event_list") },
                    onChatClicked = { navController.navigate("chat_list") }
                )
            } else {
                // Nếu userId không hợp lệ (ví dụ: đang tải), hiển thị loading
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        composable("adminPanel") {
            AdminPanelScreen(navController = navController)
        }
        composable(
            route = "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            if (!userId.isNullOrBlank()) {
                ProfileScreen(
                    userId = userId,
                    onBackClicked = { navController.popBackStack() },
                    onLogoutSuccess = {
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Các màn hình phụ khác
        composable("sos") {
            SOSScreen(
                onNavigateBack = {
                    // Lệnh này sẽ đưa người dùng quay lại màn hình trước đó
                    navController.popBackStack()
                }
            )
        }
        composable("add_friend") { AddFriendScreen(onClose = { navController.popBackStack() } ) }
        composable("event_list") {
            EventListScreen(
                onNavigateToCreateEvent = {
                    navController.navigate("create_event")
                },
                // --- TRUYỀN HÀNH ĐỘNG QUAY LẠI VÀO ĐÂY ---
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 2. Route cho màn hình TẠO MỚI sự kiện
        composable("create_event") {
            CreateEventScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("chat_list") {
            ChatListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onChatItemClicked = { chatId ->
                    navController.navigate("chat_detail/$chatId")
                },
                onNavigateToCreateGroup = {
                    navController.navigate("create_group")
                }
            )
        }
        composable("create_group") {
            CreateGroupScreen(
                onNavigateBack = { navController.popBackStack() },
                onGroupCreated = { newChatId ->
                    // Sau khi tạo nhóm thành công, đi thẳng vào màn hình chat của nhóm đó
                    // và xóa màn hình "CreateGroup" khỏi lịch sử back stack
                    navController.navigate("chat_detail/$newChatId") {
                        popUpTo("create_group") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "chat_detail/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Lời gọi này đã đúng, không cần truyền tham số nữa
            ChatDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
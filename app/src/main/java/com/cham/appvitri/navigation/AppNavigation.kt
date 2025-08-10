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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // startDestination LUÔN LUÔN là "splash"
    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen() // Hiển thị màn hình chờ
            LaunchedEffect(key1 = true) {
                if (auth.currentUser != null) {
                    navController.navigate("home/${auth.currentUser!!.uid}") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("welcome") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }

        composable("welcome") { WelcomeScreen(navController = navController) }

        composable("login") {
            LoginScreen(
                navController = navController,
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
                    onEventsClicked = { navController.navigate("events") },
                    onChatClicked = { navController.navigate("chat_list") }
                )
            } else {
                // Nếu userId không hợp lệ (ví dụ: đang tải), hiển thị loading
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
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
        composable("sos") { SOSScreen() }
        composable("add_friend") { AddFriendScreen(onClose = { navController.popBackStack() } ) }
        composable("events") { EventListScreen() }
        composable("chat_list") { ChatListScreen() }
    }
}
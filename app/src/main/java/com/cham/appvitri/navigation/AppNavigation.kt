package com.cham.appvitri.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cham.appvitri.view.AddFriendScreen
import com.cham.appvitri.view.ChatListScreen
import com.cham.appvitri.view.EventListScreen
import com.cham.appvitri.view.HomeScreen
import com.cham.appvitri.view.ProfileScreen
import com.cham.appvitri.view.SOSScreen
import com.cham.appvitri.view.WelcomeScreen
import com.cham.appvitri.view.account.LoginScreen
import com.cham.appvitri.view.login.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // KIỂM TRA ĐIỂM 1: startDestination có đúng định dạng không?
    val startDestination = if (auth.currentUser != null) {
        // PHẢI CÓ ĐỊNH DẠNG "home/..."
        "home/${auth.currentUser!!.uid}"  } else { "welcome" }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(route = "welcome") {
            WelcomeScreen(navController = navController)
        }

        composable(route = "login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = { userId ->
                    // Đây là nơi bạn sẽ gọi navigate, chúng ta sẽ kiểm tra nó ở Checklist 2
                    navController.navigate("home/$userId") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        composable(route = "register") {
            RegisterScreen(navController = navController)
        }

        // KIỂM TRA ĐIỂM 2: ĐỊNH NGHĨA ROUTE "HOME" - QUAN TRỌNG NHẤT
        // Route có phải là "home/{userId}" không?
        // Tham số 'userId' có được định nghĩa trong arguments không?
        composable(
            route = "home/{userId}", // <<< PHẢI CÓ {userId}
            arguments = listOf(navArgument("userId") {// <<< PHẢI CÓ KHAI BÁO ARGUMENT
                type = NavType.StringType
            })
        ) { backStackEntry ->
            // Lấy userId ra từ backStackEntry
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            HomeScreen(userId = userId,
                // khi homescreen báo cáo avtar được click
        onAvatarClicked= {navController.navigate("profile/$userId")},
            onSosClicked = {
                // ...thì AppNavigation sẽ điều hướng đến màn hình sos.
                navController.navigate("sos")
            },
            onAddFriendClicked = {
                navController.navigate("add_friend")
            },
            onEventsClicked = {
                navController.navigate("events")
            },
            onChatClicked = {
                navController.navigate("chat_list")
            }
            )
        }
        // --- Định nghĩa các màn hình mà HomeScreen có thể điều hướng tới ---
        composable("profile/{userId}") { /* Màn hình Profile của bạn */ ProfileScreen(navController, it.arguments?.getString("userId"))}
        composable("sos") { /* Màn hình SOS của bạn */ SOSScreen() }
        composable("add_friend") { /* Màn hình Add Friend của bạn */AddFriendScreen() }
        composable("events") { /* Màn hình Events của bạn */ EventListScreen() }
        composable("chat_list") { /* Màn hình Chat List của bạn */ ChatListScreen() }
    }
}
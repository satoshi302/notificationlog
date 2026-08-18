package com.example.notificationlog.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notificationlog.App
import com.example.notificationlog.ui.chat.ChatScreen
import com.example.notificationlog.ui.home.ConversationListScreen
import com.example.notificationlog.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val CHAT = "chat/{key}/{title}"
    fun chat(key: String, title: String): String =
        "chat/${Uri.encode(key)}/${Uri.encode(title)}"
}

@Composable
fun AppNav(app: App) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            ConversationListScreen(
                app = app,
                onOpenConversation = { convo ->
                    navController.navigate(Routes.chat(convo.conversationKey, convo.conversationTitle))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("key") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val key = backStackEntry.arguments?.getString("key")?.let(Uri::decode).orEmpty()
            val title = backStackEntry.arguments?.getString("title")?.let(Uri::decode).orEmpty()
            ChatScreen(
                app = app,
                conversationKey = key,
                title = title,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
